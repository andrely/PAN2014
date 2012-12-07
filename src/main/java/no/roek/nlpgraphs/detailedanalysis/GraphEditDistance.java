package no.roek.nlpgraphs.detailedanalysis;

import java.util.List;
import java.util.Map;

import no.roek.nlpgraphs.graph.Edge;
import no.roek.nlpgraphs.graph.Graph;
import no.roek.nlpgraphs.graph.Node;

import com.google.code.javakbest.JVC;
import com.konstantinosnedas.HungarianAlgorithm;


public class GraphEditDistance {

	private double[][] costMatrix;
	protected final double SUBSTITUTE_COST;
	protected final double INSERT_COST;
	protected final double DELETE_COST;
	private Graph g1, g2;
	private Map<String, Double> posEditWeights, deprelEditWeights;

	public GraphEditDistance(Graph g1, Graph g2, double subCost, double insCost, double delCost, Map<String, Double> posEditWeights, Map<String, Double> deprelEditWeights) {
		this.SUBSTITUTE_COST = subCost;
		this.INSERT_COST = insCost;
		this.DELETE_COST = delCost;
		this.g1 = g1;
		this.g2 = g2;
		this.posEditWeights = posEditWeights;
		this.deprelEditWeights = deprelEditWeights;
		this.costMatrix = createCostMatrix(g1, g2);
	}

	public GraphEditDistance(Graph g1, Graph g2, Map<String, Double> posEditWeights, Map<String, Double> deprelEditWeights) {
		this(g1, g2, 2, 1, 1, posEditWeights, deprelEditWeights);
	}

	public double getNormalizedDistance() {
		/**
		 * Retrieves the approximated graph edit distance between the two graphs g1 & g2.
		 * The distance is normalized on graph length
		 */
		double graphLength = (g1.getSize()+g2.getSize())/2;
		return getDistance() / graphLength;
	}

	public double getDistance() {
		/**
		 * Retrieves the approximated graph edit distance between the two graphs g1 & g2.
		 */
		this.costMatrix = createCostMatrix(g1, g2);
		int[][] assignment = HungarianAlgorithm.hgAlgorithm(this.costMatrix, "min");

		double sum = 0; 
		for (int i=0; i<assignment.length; i++){
			sum =  (sum + costMatrix[assignment[i][0]][assignment[i][1]]);
		}

		return sum;
	}
	public double[][] getCostMatrix() {
		if(costMatrix==null) {
			this.costMatrix = createCostMatrix(g1, g2);
		}
		return costMatrix;
	}

//	public double getDistance() {
//		/**
//		 * Retrieves the graph edit distance of graph g1 & g2,
//		 * using the Jonker-Volgenant algorithm to retrieve the optimal cost assignment of the cost matrix.
//		 */
//		JVC jvc = JVC.solve(this.costMatrix);
//		return jvc.getCost();
//	}

	private double[][] createCostMatrix(Graph g1, Graph g2) {
		/**
		 * Creates the cost matrix used as input to Munkres algorithm.
		 * The matrix consists of 4 sectors: upper left, upper right, bottom left, bottom right.
		 * Upper left represents the cost of all N x M node substitutions. 
		 * Upper right node deletions
		 * Bottom left node insertions. 
		 * Bottom right represents delete -> delete operations which should have any cost, and is filled with zeros.
		 */
		int n = g1.getNodes().size();
		int m = g2.getNodes().size();
		double[][] costMatrix = new double[n+m][n+m];

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				costMatrix[i][j] = getSubstituteCost(g1.getNode(i), g2.getNode(j));
			}
		}

		for (int i = 0; i < m; i++) {
			for (int j = 0; j < m; j++) {
				costMatrix[i+n][j] = getInsertCost(i, j);
			}
		}

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				costMatrix[j][i+m] = getDeleteCost(i, j);
			}
		}

		return costMatrix;
	}

	private double getInsertCost(int i, int j) {
		if(i == j) {
			return getPosWeight(g2.getNode(j)) * INSERT_COST;
		}
		return Double.MAX_VALUE;
	}

	private double getDeleteCost(int i, int j) {
		if(i == j) {
			return getPosWeight(g1.getNode(i)) * DELETE_COST;
		}
		return Double.MAX_VALUE;
	}

	public double getSubstituteCost(Node node1, Node node2) {

		double diff = (getRelabelCost(node1, node2) + getEdgeDiff(node1, node2)) / 2;

		return diff * SUBSTITUTE_COST;
	}

	public double getRelabelCost(Node node1, Node node2) {
		double diff = 0;
		if(!node1.equals(node2)) {
			diff = getPosWeight(node1, node2);
		}

		return diff ;
	}

	public double getPosWeight(Node node) {
		return getPosWeight(node.getAttributes().get(0));
	}

	public double getPosWeight(Node node1, Node node2) {
		return getPosWeight(node1.getAttributes().get(0)+","+node2.getAttributes().get(0));
	}

	public double getPosWeight(String key) {
		Double posWeight = posEditWeights.get(key);
		if(posWeight == null) {
			return 1;
		}	
		return posWeight;
	}

	public double getEdgeDiff(Node node1, Node node2) {
		List<Edge> edges1 = g1.getEdges(node1);
		List<Edge> edges2 = g2.getEdges(node2);
		if(edges1.size() == 0 || edges2.size() == 0) {
			return getWeightSum(edges1) + getWeightSum(edges2);
		}

		int n = edges1.size();
		int m = edges2.size();
		double[][] edgeCostMatrix = new double[n+m][m+n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				edgeCostMatrix[i][j] = getEdgeEditCost(edges1.get(i), edges2.get(j));
			}
		}

		for (int i = 0; i < m; i++) {
			for (int j = 0; j < m; j++) {
				edgeCostMatrix[i+n][j] = getEdgeInsertCost(i, j, edges2.get(j));
			}
		}

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				edgeCostMatrix[j][i+m] = getEdgeDeleteCost(i, j, edges1.get(i));
			}
		}

		int[][] assignment = HungarianAlgorithm.hgAlgorithm(edgeCostMatrix, "min");
		double sum = 0; 
		for (int i=0; i<assignment.length; i++){
			sum += edgeCostMatrix[assignment[i][0]][assignment[i][1]];
		}

		return sum / ((n+m));
	}

	public double getDeprelWeight(Edge edge) {
		Double weight = deprelEditWeights.get(edge.getLabel());
		if(weight == null) {
			return 1;
		}
		return weight;
	}

	public double getWeightSum(List<Edge> edges) {
		double sum = 0;
		for (Edge edge : edges) {
			sum += getDeprelWeight(edge);
		}

		return sum;
	}

	public double getEdgeInsertCost(int i, int j, Edge edge2) {
		if(i==j) {
			return getDeprelWeight(edge2) * INSERT_COST;
		}
		return Double.MAX_VALUE;
	}

	public double getEdgeDeleteCost(int i, int j, Edge edge1) {
		if(i==j) {
			return getDeprelWeight(edge1) * DELETE_COST;
		}
		return Double.MAX_VALUE;
	}

	public double getEdgeEditCost(Edge edge1, Edge edge2) {
		if(edge1.equals(edge2)) {
			return 0;
		}
		return 1;
	}


	public void printMatrix() {
		if(costMatrix == null) {
			costMatrix = createCostMatrix(g1, g2);
		}
		System.out.println("-------------");
		System.out.println("Cost matrix: ");
		for (int i = 0; i < costMatrix.length; i++) {
			for (int j = 0; j < costMatrix.length; j++) {
				if(costMatrix[i][j] == Double.MAX_VALUE) {
					System.out.print("inf\t");
				}else{
					System.out.print(String.format("%.2f", costMatrix[i][j])+"\t");
				}
			}
			System.out.println();
		}
	}


}
