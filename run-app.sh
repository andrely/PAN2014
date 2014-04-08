java -Xms4G -cp `mvn dependency:build-classpath | egrep -v "(^\[INFO\]|^\[DEBUG\]|^\[WARNING\])"`:target/text2graph-1.0-SNAPSHOT.jar no.roek.nlpgraphs.application.App "$@"
