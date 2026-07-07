mvn versions:set -DnewVersion=1.0.13 -DgenerateBackupPoms=false -DprocessAllModules=true
mvn clean  install -P release