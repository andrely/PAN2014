java -Xms4G -cp target/text2graph-1.0-SNAPSHOT.jar:`mvn dependency:build-classpath | egrep -v "(^\[INFO\]|^\[DEBUG\]|^\[WARNING\])"` no.roek.nlpgraphs.application.App "$@"
