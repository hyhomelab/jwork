mvn versions:set -DnewVersion=1.0.4 -DgenerateBackupPoms=false -DprocessAllModules=true
mvn clean  deploy -P release