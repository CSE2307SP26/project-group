#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src/main/java"
BUILD_DIR="$SCRIPT_DIR/build/manual-classes"
LIB_DIR="$SCRIPT_DIR/lib"
SQLITE_JDBC_VERSION="3.47.2.0"
SQLITE_JDBC_JAR="sqlite-jdbc-${SQLITE_JDBC_VERSION}.jar"
SQLITE_JDBC_PATH="$LIB_DIR/$SQLITE_JDBC_JAR"
SQLITE_JDBC_URL="https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/${SQLITE_JDBC_VERSION}/${SQLITE_JDBC_JAR}"
USE_WINDOWS_JAVA=false
JAVAC_BIN=""
JAVA_BIN=""
JAVA_RUNTIME_OPTS=()

detect_java_tools() {
    if command -v javac >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
        JAVAC_BIN="javac"
        JAVA_BIN="java"
        return
    fi

    if command -v javac.exe >/dev/null 2>&1 && command -v java.exe >/dev/null 2>&1; then
        USE_WINDOWS_JAVA=true
        JAVAC_BIN="javac.exe"
        JAVA_BIN="java.exe"
        return
    fi

    if command -v powershell.exe >/dev/null 2>&1 && command -v wslpath >/dev/null 2>&1; then
        local windows_javac_path
        local windows_java_path

        windows_javac_path="$(powershell.exe -NoProfile -Command "(Get-Command javac).Source" | tr -d '\r')"
        windows_java_path="$(powershell.exe -NoProfile -Command "(Get-Command java).Source" | tr -d '\r')"

        if [[ -n "$windows_javac_path" && -n "$windows_java_path" ]]; then
            USE_WINDOWS_JAVA=true
            JAVAC_BIN="$(wslpath -u "$windows_javac_path")"
            JAVA_BIN="$(wslpath -u "$windows_java_path")"
            return
        fi
    fi

    if command -v cmd.exe >/dev/null 2>&1 && command -v wslpath >/dev/null 2>&1; then
        local windows_javac_path
        local windows_java_path

        windows_javac_path="$(cmd.exe /c where javac 2>/dev/null | tr -d '\r' | head -n 1)"
        windows_java_path="$(cmd.exe /c where java 2>/dev/null | tr -d '\r' | head -n 1)"

        if [[ -n "$windows_javac_path" && -n "$windows_java_path" ]]; then
            USE_WINDOWS_JAVA=true
            JAVAC_BIN="$(wslpath -u "$windows_javac_path")"
            JAVA_BIN="$(wslpath -u "$windows_java_path")"
            return
        fi
    fi

    if [[ -x "/mnt/c/Program Files/Common Files/Oracle/Java/javapath/javac.exe" \
        && -x "/mnt/c/Program Files/Common Files/Oracle/Java/javapath/java.exe" ]]; then
        USE_WINDOWS_JAVA=true
        JAVAC_BIN="/mnt/c/Program Files/Common Files/Oracle/Java/javapath/javac.exe"
        JAVA_BIN="/mnt/c/Program Files/Common Files/Oracle/Java/javapath/java.exe"
        return
    fi

    echo "Error: javac and java were not found. Install a JDK before running the app." >&2
    exit 1
}

is_valid_jar() {
    [[ -s "$SQLITE_JDBC_PATH" ]]
}

download_sqlite_jdbc() {
    local temp_path="${SQLITE_JDBC_PATH}.part"

    echo "Downloading $SQLITE_JDBC_JAR ..."
    rm -f "$temp_path"

    if command -v powershell.exe >/dev/null 2>&1 && command -v wslpath >/dev/null 2>&1; then
        local windows_temp_path
        windows_temp_path="$(wslpath -w "$temp_path")"
        powershell.exe -NoProfile -Command \
            "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '$SQLITE_JDBC_URL' -OutFile '$windows_temp_path'"
        mv "$temp_path" "$SQLITE_JDBC_PATH"
        return
    fi

    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --output "$temp_path" "$SQLITE_JDBC_URL"
        mv "$temp_path" "$SQLITE_JDBC_PATH"
        return
    fi

    if command -v wget >/dev/null 2>&1; then
        wget --output-document="$temp_path" "$SQLITE_JDBC_URL"
        mv "$temp_path" "$SQLITE_JDBC_PATH"
        return
    fi

    echo "Error: PowerShell, curl, or wget is required to download the SQLite JDBC dependency." >&2
    exit 1
}

build_classpath() {
    local class_output="$BUILD_DIR"
    local jdbc_jar="$SQLITE_JDBC_PATH"

    if [[ "$USE_WINDOWS_JAVA" == true ]] && command -v wslpath >/dev/null 2>&1; then
        class_output="$(wslpath -m "$BUILD_DIR")"
        jdbc_jar="$(wslpath -m "$SQLITE_JDBC_PATH")"
        printf '%s;%s' "$class_output" "$jdbc_jar"
        return
    fi

    case "$(uname -s)" in
        CYGWIN*|MINGW*|MSYS*)
            if command -v cygpath >/dev/null 2>&1; then
                class_output="$(cygpath -w "$BUILD_DIR")"
                jdbc_jar="$(cygpath -w "$SQLITE_JDBC_PATH")"
            fi
            printf '%s;%s' "$class_output" "$jdbc_jar"
            ;;
        *)
            printf '%s:%s' "$class_output" "$jdbc_jar"
            ;;
    esac
}

configure_runtime_options() {
    if [[ -z "${BANK_DB_FILE:-}" ]]; then
        return
    fi

    local db_file="$BANK_DB_FILE"
    if [[ "$USE_WINDOWS_JAVA" == true ]] \
        && command -v wslpath >/dev/null 2>&1 \
        && [[ ! "$db_file" =~ ^[A-Za-z]:[\\/] ]]; then
        db_file="$(wslpath -m "$db_file")"
    fi

    JAVA_RUNTIME_OPTS+=("-Dbank.db.file=$db_file")
}

write_source_list() {
    local source_list_file="$BUILD_DIR/java_sources.txt"

    : > "$source_list_file"
    for java_file in "${JAVA_FILES[@]}"; do
        if [[ "$USE_WINDOWS_JAVA" == true ]] && command -v wslpath >/dev/null 2>&1; then
            printf '"%s"\n' "$(wslpath -m "$java_file")" >> "$source_list_file"
        else
            printf '"%s"\n' "$java_file" >> "$source_list_file"
        fi
    done

    printf '%s' "$source_list_file"
}

detect_java_tools
configure_runtime_options
mkdir -p "$LIB_DIR"

if ! is_valid_jar; then
    rm -f "$SQLITE_JDBC_PATH"
    download_sqlite_jdbc
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

JAVA_FILES=()
while IFS= read -r -d '' file; do
    JAVA_FILES+=("$file")
done < <(find "$SRC_DIR" -name "*.java" -print0)

if [[ ${#JAVA_FILES[@]} -eq 0 ]]; then
    echo "Error: no Java source files found under $SRC_DIR" >&2
    exit 1
fi

echo "Compiling Java sources ..."
SOURCE_LIST_FILE="$(write_source_list)"
OUTPUT_DIR="$BUILD_DIR"
COMPILE_CLASSPATH="$SQLITE_JDBC_PATH"

if [[ "$USE_WINDOWS_JAVA" == true ]] && command -v wslpath >/dev/null 2>&1; then
    SOURCE_LIST_FILE="$(wslpath -m "$SOURCE_LIST_FILE")"
    OUTPUT_DIR="$(wslpath -m "$BUILD_DIR")"
    COMPILE_CLASSPATH="$(wslpath -m "$SQLITE_JDBC_PATH")"
fi

"$JAVAC_BIN" -cp "$COMPILE_CLASSPATH" -d "$OUTPUT_DIR" @"$SOURCE_LIST_FILE"

echo "Running app ..."
"$JAVA_BIN" "${JAVA_RUNTIME_OPTS[@]}" -cp "$(build_classpath)" edu.washu.bank.App "$@"
