#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="${SCRIPT_DIR}/arcusdb"
BINDING_NAME="arcusdb"
BINDING_CLASS="com.jam2in.arcusdb.ycsb.ArcusYcsbClient"
JAR_NAME="arcusdb-ycsb-binding.jar"
YCSB_VERSION="0.17.0"
YCSB_ARCHIVE="ycsb-${YCSB_VERSION}.tar.gz"
YCSB_DOWNLOAD_URL="https://github.com/brianfrankcooper/YCSB/releases/download/${YCSB_VERSION}/${YCSB_ARCHIVE}"
WORKLOAD_SOURCE_DIR="${SCRIPT_DIR}/scripts/workloads"

usage() {
  cat <<EOF
Usage:
  ./build.sh
  ./build.sh /path/to/ycsb-0.17.0

or:
  YCSB_HOME=/path/to/ycsb-0.17.0 ./build.sh

If YCSB_HOME is not provided, this script uses ./ycsb-0.17.0.
If that directory does not exist, it downloads YCSB ${YCSB_VERSION} first.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

YCSB_HOME="${1:-${YCSB_HOME:-${SCRIPT_DIR}/ycsb-${YCSB_VERSION}}}"

if [[ ! -d "${YCSB_HOME}" ]]; then
  echo "YCSB was not found at ${YCSB_HOME}."
  echo "Downloading YCSB ${YCSB_VERSION}..."

  YCSB_PARENT="$(cd "$(dirname "${YCSB_HOME}")" && pwd)"
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl was not found. Install curl or download YCSB manually." >&2
    exit 1
  fi

  curl -fL -o "${YCSB_PARENT}/${YCSB_ARCHIVE}" "${YCSB_DOWNLOAD_URL}"
  tar xzf "${YCSB_PARENT}/${YCSB_ARCHIVE}" -C "${YCSB_PARENT}"
fi

if [[ ! -d "${YCSB_HOME}/lib" ]]; then
  echo "YCSB lib directory was not found: ${YCSB_HOME}/lib" >&2
  echo "Check that the path points to an extracted YCSB distribution." >&2
  exit 1
fi

if [[ -x "${YCSB_HOME}/bin/ycsb.sh" ]]; then
  YCSB_BIN="./bin/ycsb.sh"
elif [[ -x "${YCSB_HOME}/bin/ycsb" ]]; then
  YCSB_BIN="./bin/ycsb"
else
  echo "YCSB executable was not found under ${YCSB_HOME}/bin." >&2
  echo "Expected bin/ycsb.sh or bin/ycsb." >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven was not found. Install Maven 3.x first." >&2
  exit 1
fi

echo "[1/3] Building ArcusDB YCSB binding..."
mvn -f "${MODULE_DIR}/pom.xml" -DskipTests package

BUILT_JAR="${MODULE_DIR}/target/${JAR_NAME}"
if [[ ! -f "${BUILT_JAR}" ]]; then
  echo "Expected jar was not created: ${BUILT_JAR}" >&2
  exit 1
fi

echo "[2/3] Installing binding jar into YCSB..."
YCSB_BINDING_DIR="${YCSB_HOME}/${BINDING_NAME}-binding"
YCSB_BINDING_LIB_DIR="${YCSB_BINDING_DIR}/lib"
mkdir -p "${YCSB_BINDING_LIB_DIR}"
cp "${BUILT_JAR}" "${YCSB_BINDING_LIB_DIR}/${JAR_NAME}"

BINDINGS_FILE="${YCSB_HOME}/bin/bindings.properties"
BINDINGS_TMP="${BINDINGS_FILE}.tmp"
if [[ -f "${BINDINGS_FILE}" ]]; then
  grep -v "^${BINDING_NAME}:" "${BINDINGS_FILE}" > "${BINDINGS_TMP}" || true
else
  : > "${BINDINGS_TMP}"
fi
printf "%s:%s\n" "${BINDING_NAME}" "${BINDING_CLASS}" >> "${BINDINGS_TMP}"
mv "${BINDINGS_TMP}" "${BINDINGS_FILE}"

echo "[3/3] Installing ArcusDB workloads into YCSB..."
if [[ -d "${WORKLOAD_SOURCE_DIR}" ]]; then
  mkdir -p "${YCSB_HOME}/workloads"
  cp "${WORKLOAD_SOURCE_DIR}"/* "${YCSB_HOME}/workloads/"
fi

WRAPPER="${YCSB_HOME}/bin/ycsb-arcusdb"
cat > "${WRAPPER}" <<EOF
#!/usr/bin/env bash
set -euo pipefail

YCSB_HOME="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")/.." && pwd)"
BINDING_NAME="${BINDING_NAME}"
BINDING_CLASS="${BINDING_CLASS}"

usage() {
  cat <<USAGE
Usage:
  ./bin/ycsb-arcusdb load [YCSB options]
  ./bin/ycsb-arcusdb run  [YCSB options]

Example:
  ./bin/ycsb-arcusdb load -P workloads/arcusdb-workloadc -p arcusdb.host=127.0.0.1 -p arcusdb.port=17191 -s
USAGE
}

if [[ \$# -eq 0 || "\${1:-}" == "-h" || "\${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

COMMAND="\$1"
shift

case "\${COMMAND}" in
  load)
    MODE="-load"
    ;;
  run)
    MODE="-t"
    ;;
  *)
    echo "Unknown command: \${COMMAND}" >&2
    usage >&2
    exit 1
    ;;
esac

cd "\${YCSB_HOME}"
exec java -cp "conf:lib/*:\${BINDING_NAME}-binding/lib/*" site.ycsb.Client "\${MODE}" -db "\${BINDING_CLASS}" "\$@"
EOF
chmod +x "${WRAPPER}"

cat <<EOF

Done.

Installed:
  ${YCSB_BINDING_LIB_DIR}/${JAR_NAME}
  ${WRAPPER}
  ${BINDINGS_FILE} entry: ${BINDING_NAME}:${BINDING_CLASS}
  ${YCSB_HOME}/workloads/ files from ${WORKLOAD_SOURCE_DIR}

The wrapper already uses this DB class:
  ${BINDING_CLASS}

Example:
  cd "${YCSB_HOME}"
  ./bin/ycsb-arcusdb load -P workloads/arcusdb-workloadc -p arcusdb.host=127.0.0.1 -p arcusdb.port=17191 -s
  ./bin/ycsb-arcusdb run  -P workloads/arcusdb-workloadc -p arcusdb.host=127.0.0.1 -p arcusdb.port=17191 -s

Raw YCSB command, if you prefer it:
  ${YCSB_BIN} load ${BINDING_NAME} -P workloads/arcusdb-workloadc -p arcusdb.host=127.0.0.1 -p arcusdb.port=17191 -s
EOF
