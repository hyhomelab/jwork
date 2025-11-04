mvn versions:set -DnewVersion=1.0.7 -DgenerateBackupPoms=false -DprocessAllModules=true
mvn clean  install -P release