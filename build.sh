mvn versions:set -DnewVersion=1.0.14 -DgenerateBackupPoms=false -DprocessAllModules=true
mvn clean  install -P release