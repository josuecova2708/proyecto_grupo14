#!/bin/bash
# Script de producción — ejecutar en el servidor como grupo14sa
JAVA="$HOME/.local/jdk-21.0.3+9/bin/java"
JAR="$HOME/MoronCorreo-1.0.jar"
LOG="$HOME/moron.log"

echo "=== $(date '+%Y-%m-%d %H:%M:%S') ===" >> "$LOG"
"$JAVA" -DMODE=SERVER -jar "$JAR" >> "$LOG" 2>&1
