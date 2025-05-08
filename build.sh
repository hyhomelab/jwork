mvn versions:set -DnewVersion=1.0.1 -DgenerateBackupPoms=false -DprocessAllModules=true
mvn clean  deploy -P release