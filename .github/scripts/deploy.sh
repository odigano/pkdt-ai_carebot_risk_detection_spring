#!/bin/bash
set -eux # 명령 실패 시 즉시 종료, 실행 명령 출력

REMOTE_DEPLOY_PATH="$1"
REMOTE_JAR_FILENAME="$2"
REMOTE_DB_URL="$3"
REMOTE_DB_USERNAME="$4"
REMOTE_DB_PASSWORD="$5"

NEW_RELEASE_DIR="$REMOTE_DEPLOY_PATH/release/$(date +%Y%m%d%H%M%S)"
CURRENT_SYMLINK="$REMOTE_DEPLOY_PATH/current"

echo "--- REMOTE SERVER SCRIPT START ---"
echo "  Received DEPLOY_PATH: $REMOTE_DEPLOY_PATH"
echo "  Received JAR_FILENAME: $REMOTE_JAR_FILENAME"

echo "--- REMOTE SERVER: Creating release directory $NEW_RELEASE_DIR ---"
mkdir -p "$NEW_RELEASE_DIR"

echo "--- REMOTE SERVER: Moving JAR file to $NEW_RELEASE_DIR/$REMOTE_JAR_FILENAME ---"
mv "$REMOTE_DEPLOY_PATH/$REMOTE_JAR_FILENAME" "$NEW_RELEASE_DIR/$REMOTE_JAR_FILENAME"

echo "--- REMOTE SERVER: Updating symbolic link ---"
ln -sfn "$NEW_RELEASE_DIR" "$CURRENT_SYMLINK"

echo "--- REMOTE SERVER: Checking for existing Java processes with $CURRENT_SYMLINK/$REMOTE_JAR_FILENAME ---"
PID=$(pgrep -f "java -jar $CURRENT_SYMLINK/$REMOTE_JAR_FILENAME")
if [ -n "$PID" ]; then
  echo "--- REMOTE SERVER: Found existing process (PID: $PID). Attempting graceful shutdown (SIGTERM)... ---"
  kill -15 "$PID"
  for i in {1..5}; do
    if ! kill -0 "$PID" 2>/dev/null; then
      echo "--- REMOTE SERVER: Process $PID terminated gracefully. ---"
      break
    fi
    echo "--- REMOTE SERVER: Waiting for process $PID to terminate... ($i/5) ---"
    sleep 1
  done
  if kill -0 "$PID" 2>/dev/null; then
    echo "--- REMOTE SERVER: Process $PID did not terminate gracefully, forcing kill with SIGKILL. ---"
    kill -9 "$PID"
  fi
else
  echo "--- REMOTE SERVER: No existing process found for $CURRENT_SYMLINK/$REMOTE_JAR_FILENAME ---"
fi

echo "--- REMOTE SERVER: Verifying no old Java processes are running. ---"
REMAINING_PIDS=$(pgrep -f "java -jar $CURRENT_SYMLINK/$REMOTE_JAR_FILENAME")
if [ -n "$REMAINING_PIDS" ]; then
  echo "Error on server: Old Java processes still running after termination attempt: $REMAINING_PIDS"
  exit 1
else
  echo "--- REMOTE SERVER: All old Java processes for $CURRENT_SYMLINK/$REMOTE_JAR_FILENAME successfully terminated. ---"
fi

mkdir -p "$REMOTE_DEPLOY_PATH/logs"

export DB_URL="$REMOTE_DB_URL"
export DB_USERNAME="$REMOTE_DB_USERNAME"
export DB_PASSWORD="$REMOTE_DB_PASSWORD"

echo "  REMOTE SERVER: Starting application command: nohup /usr/bin/java -jar $CURRENT_SYMLINK/$REMOTE_JAR_FILENAME > $REMOTE_DEPLOY_PATH/logs/$REMOTE_JAR_FILENAME.log 2>&1 &"
nohup /usr/bin/java -jar "$CURRENT_SYMLINK/$REMOTE_JAR_FILENAME" > "$REMOTE_DEPLOY_PATH/logs/$REMOTE_JAR_FILENAME.log" 2>&1 &

echo "--- REMOTE SERVER: Verifying new application started successfully... ---"
APP_STARTED=false
for i in {1..3}; do
  sleep 5
  NEW_PID=$(pgrep -f "java -jar $CURRENT_SYMLINK/$REMOTE_JAR_FILENAME")
  if [ -n "$NEW_PID" ]; then
    echo "--- REMOTE SERVER: New application process found (PID: $NEW_PID) after ($((i*5))) seconds. ---"
    APP_STARTED=true
    break
  fi
  echo "--- REMOTE SERVER: Waiting for new application to start... ($i/3) ---"
done

if [ "$APP_STARTED" = "false" ]; then
  echo "Error on server: New application did not start successfully."
  echo "  Check logs for more details: tail -n 50 $REMOTE_DEPLOY_PATH/logs/$REMOTE_JAR_FILENAME.log"
  exit 1
else
  echo "--- REMOTE SERVER: New application started successfully. PID: $NEW_PID ---"
fi

echo "--- REMOTE SERVER: Starting old release cleanup ---"
CURRENT_RELEASE_TARGET=$(readlink -f "$CURRENT_SYMLINK" || echo "")
if [ -z "$CURRENT_RELEASE_TARGET" ]; then
  echo "Warning: Could not determine current release target for cleanup. Skipping old release cleanup."
else
  OLD_RELEASES=$(ls -dt "$REMOTE_DEPLOY_PATH/release/"* 2>/dev/null | grep -v "$CURRENT_RELEASE_TARGET" | tail -n +5)
  if [ -n "$OLD_RELEASES" ]; then
    echo "--- REMOTE SERVER: Found old releases to delete: ---"
    echo "$OLD_RELEASES"
    for RELEASE_TO_DELETE in $OLD_RELEASES; do
      echo "--- REMOTE SERVER: Deleting old release directory: $RELEASE_TO_DELETE ---"
      rm -rf "$RELEASE_TO_DELETE"
    done
    echo "--- REMOTE SERVER: Old release cleanup completed. ---"
  else
    echo "--- REMOTE SERVER: No old releases found to delete or less than 5 releases. ---"
  fi
fi

echo "--- REMOTE SERVER SCRIPT END ---"
