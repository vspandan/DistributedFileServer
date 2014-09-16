javac *.java
java -Djava.security.policy=rmi.policy RegistryServ $1 $2
