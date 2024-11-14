import sys
import pandas as pd
import argparse
from pathlib import Path


# Simple process to read in a csv file, filter duplicates and write out to a new file.
# python filter.py linkedInAlerts-$(TODAY).csv
# this will output to a file called: linkedInAlerts-$(TODAY)-filtered.csv

def generate_output(given_file, dataframe):
    output_file = Path(given_file.parent, "{0}-{2}{1}".format(given_file.stem, given_file.suffix, "filtered"))
    print("Outputting data to file:", output_file)
    dataframe.to_csv(output_file, index=False, header=False)

def process_csv_file(given_file):
    colnames=['Date', 'title', 'Company', 'Location', 'Description', 'Link']
    original_data_df = pd.read_csv(given_file, names=colnames, header=None)
    original_length = len(original_data_df.index)
    deduped_df = original_data_df.drop_duplicates(subset=['Link'], keep='first')
    new_length=len(deduped_df.index)
    print(f"Rows reduced from: {original_length} to {new_length} rows")
    generate_output(given_file, deduped_df)

def main():
    parser = argparse.ArgumentParser(description='Remove duplicates from CSV file')
    parser.add_argument('input_file', help='Input file name')
    args = parser.parse_args()
    print("Starting, parsing file:", args.input_file)
    csv_input_file = Path(args.input_file)
    if not csv_input_file.is_file():
        print("The given file does not exist!", file=sys.stderr)
        raise SystemExit(1)
    process_csv_file(csv_input_file)
    print("Done")


if __name__ == '__main__':
    main()
