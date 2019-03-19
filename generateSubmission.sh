!/bin/bash
usage()
{
  echo "Usage: $0 [-o --overwrite] [-d PUBLISH_DATE (yyyy-mm-dd)] [-g PUBLISH-GROUP] [-s SCV-FILE]"
  exit 2
}

exists()
{
  for file in "$@"; do
    if [[ -f $file ]]; then
      # file found, return true
      return 0
    fi
  done
  return -1
}

#########################
# Main script starts here

unset PUBLISH_DATE PUBLISH_GROUP SCV_FILE
OVERWRITE=false

options=':od:g:s:h'
while getopts $options option
do
    case $option in
        o  ) OVERWRITE=true;;
        d  ) PUBLISH_DATE=$OPTARG;;
        g  ) PUBLISH_GROUP=$OPTARG;;
        s  ) SCV_FILE=$OPTARG;;
        h  ) usage; exit;;
        \? ) echo "Unknown option: -$OPTARG" >&2; exit 1;;
        :  ) echo "Missing option argument for -$OPTARG" >&2; exit 1;;
        *  ) echo "Unimplemented option: -$OPTARG" >&2; exit 1;;
    esac
done

shift $(($OPTIND - 1))

# make sure the publish_date and publish_group argument are passed
if [ -z "$PUBLISH_DATE" -o -z "$PUBLISH_GROUP" ]
then
    echo "Both -d publish_date and -g publish_group are required."
    exit 1
fi

PUBLISH_GROUP=$(echo $PUBLISH_GROUP | awk '{print toupper($0)}')

tmpdir="tmpwork"
inputgzip="$PUBLISH_GROUP-CGSEPIO-$PUBLISH_DATE.gzip"
outputjson="$PUBLISH_GROUP-ALL-$PUBLISH_DATE.json"
submissioncsv="$PUBLISH_GROUP-SUBMISSION-$PUBLISH_DATE.csv"
runreportcsv="$PUBLISH_GROUP-RUN-REPORT-$PUBLISH_DATE.csv"

if [ -n "$SCV_FILE" ]; then
  if [ ! -f "$SCV_FILE" ]; then
    echo "Could not find the SCV file - $SCV_FILE"
    exit 1
  fi
fi

# make a tmp work directory and remove when finished
if [ -d "$tmpdir" ]; then
  rm -fR "$tmpdir"
fi
mkdir "$tmpdir"

# expand the cg-sepio-* json files
tar -xvf "$inputgzip" --directory "$tmpdir"

# seed file with wrapper brace and VariantInterpretation array
cd $tmpdir

rm -f tmpfile
echo '{"@context": "http://dataexchange.clinicalgenome.org/interpretation/json/context",' > "$outputjson"
echo '"VariantPathogenicityInterpretation":[' >> "$outputjson"

# Loop through product of gcsplit operation above...
for filename in cg-sepio-*;
do
    printf "%s\n" "$filename"

    sed -e '2d' "$filename" > "tmpfile"
    cat "tmpfile" >> "$outputjson"
    echo ',' >> "$outputjson"

done

# remove last comma
sed -i '' -e '$ d' "$outputjson"

# append enclosing bracket and brace to contain all VariantIntepretations.
echo ']}' >> "$outputjson"
rm -f "tmpfile"

cd ..

if [ -n "$SCV_FILE" ]; then
  lein run "-f" "-o" "$submissioncsv" "-r" "$runreportcsv" "-c" "$SCV_FILE" "$tmpdir/$outputjson"
else
  lein run "-f" "-o" "$submissioncsv" "-r" "$runreportcsv" "$tmpdir/$outputjson"
fi

# remove temp directory
if [ -d "$tmpdir" ]; then
  rm -fR "$tmpdir"
fi
