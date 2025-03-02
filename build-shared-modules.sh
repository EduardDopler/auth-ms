SHARED=(server-timings/pom.xml response-utils/pom.xml)

# "clean install" all shared modules (in parallel)
for f in "${SHARED[@]}"; do
  printf 'mvn clean install -q -f "%s"\n' "$f"
  mvn clean install -q -f "$f" &
done

wait
printf '###\n'

# "clean" all non-shared modules (in parallel)
for f in */pom.xml; do
  # skip shared modules
  if [[ " ${SHARED[*]} " =~ \ ${f}\  ]]; then
    continue
  fi
  printf 'mvn clean -q -f "%s"\n' "$f"
  mvn clean -q -f "$f" &
done

wait
