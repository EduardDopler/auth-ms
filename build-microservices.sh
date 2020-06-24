SHARED=(server-timings/pom.xml response-utils/pom.xml)
NEED_JVM=(credentials-store/pom.xml token-store/pom.xml)

if [ "x$*" = "x" ]; then
  GOALS=(package)
else
  GOALS=("$@")
fi

# remove native flags for modules needing a JVM
if [[ " ${GOALS[*]} " =~ " -Pnative " ]]; then
  NATIVE_BUILD=true
  GOALS_FOR_JVM=()
  for goal in "${GOALS[@]}"; do
    [[ $goal != "-Pnative" && ! $goal =~ ^-Dquarkus\.native.* ]] && GOALS_FOR_JVM+=("$goal")
  done
fi

# build images
for pom in */pom.xml; do
  # skip shared modules
  if [[ " ${SHARED[*]} " =~ \ ${pom}\  ]]; then
    continue
  fi
  # if native build, run without native profile for JVM-based services
  if [[ $NATIVE_BUILD && " ${NEED_JVM[*]} " =~ \ ${pom}\  ]]; then
    printf 'mvn %s -f "%s"  (Ignoring native flags because this module needs a JVM)\n' "${GOALS_FOR_JVM[*]}" "$pom"
    mvn "${GOALS_FOR_JVM[@]}" -f "$pom"
    continue
  fi
  printf 'mvn %s -f "%s"\n' "${GOALS[*]}" "$pom"
  mvn "${GOALS[@]}" -f "$pom"
done

# done if no native-container flag present
if [[ ! " ${GOALS[*]} " =~ \ -Dquarkus\.native\.container.* ]]; then
  exit 0
fi

# create docker images
printf "\n###\n"
printf "Build finished, running Docker builds now because native-container flag is present.\n"
printf "###\n"

BASE_DIR=${PWD}
for dockerdir in */src/main/docker; do
  cd "$dockerdir/../../.." || exit 1
  SERVICE_NAME=${PWD##*/}
  DOCKERFILE="../${dockerdir}/Dockerfile.native"
  if [ ! -f "${DOCKERFILE}" ]; then
    DOCKERFILE="../${dockerdir}/Dockerfile.jvm"
  fi
  printf '\ndocker build -f "%s" -t %s .\n' "${DOCKERFILE}" "${SERVICE_NAME}"
  docker build -f "${DOCKERFILE}" -t "${SERVICE_NAME}" .
  cd "$BASE_DIR" || exit 1
done
cd "$BASE_DIR" || exit 0
