SHARED=(server-timings/pom.xml)

# "clean install" all shared modules (in parallel)
for f in "${SHARED[@]}"; do
  echo mvn clean install -q -f "$f"
  mvn clean install -q -f "$f" &
done

wait
echo ---

# "clean" all non-shared modules (in parallel)
for f in */pom.xml; do
  # skip shared modules
  if [[ " ${SHARED[*]} " =~ \ ${f}\  ]]; then
    continue
  fi
   echo clean -q -f "$f"
   mvn clean -q -f "$f" &
done

wait
