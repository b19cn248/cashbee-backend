#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script to update Sub_id1 column in Shopee CSV with tracking codes
Tracking codes will alternate between CB1_1_YYYYMMDDHHMMSS and CB1_2_YYYYMMDDHHMMSS
"""

import csv
from datetime import datetime, timedelta

def update_csv_tracking_codes(input_file, output_file):
    """
    Update Sub_id1 column with tracking codes

    Args:
        input_file: Path to input CSV file
        output_file: Path to output CSV file
    """
    # Base tracking codes
    tracking_codes = [
        "CB1_1_20251103215156",
        "CB1_2_20251103215300"
    ]

    rows = []

    # Read CSV file with UTF-8 encoding (handle BOM)
    with open(input_file, 'r', encoding='utf-8-sig') as f:
        reader = csv.reader(f)
        header = next(reader)

        # Find Sub_id1 column index
        try:
            sub_id1_index = header.index('Sub_id1')
        except ValueError:
            print("Error: 'Sub_id1' column not found")
            return

        rows.append(header)

        # Process data rows
        row_count = 0
        for row in reader:
            if len(row) > sub_id1_index:
                # Alternate between two tracking codes
                tracking_code = tracking_codes[row_count % 2]
                row[sub_id1_index] = tracking_code
                row_count += 1

            rows.append(row)

    # Write updated CSV
    with open(output_file, 'w', encoding='utf-8-sig', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(rows)

    print(f"✅ Updated {row_count} rows")
    print(f"   - {(row_count + 1) // 2} rows with tracking code: {tracking_codes[0]}")
    print(f"   - {row_count // 2} rows with tracking code: {tracking_codes[1]}")
    print(f"   Output saved to: {output_file}")

if __name__ == "__main__":
    input_file = "AffiliateCommissionReport202510300813.csv"
    output_file = "AffiliateCommissionReport202510300813_updated.csv"

    update_csv_tracking_codes(input_file, output_file)
