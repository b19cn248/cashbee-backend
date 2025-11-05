#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script to verify tracking codes in Sub_id1 column
"""

import csv

def verify_tracking_codes(file_path):
    """
    Verify tracking codes in CSV file

    Args:
        file_path: Path to CSV file
    """
    with open(file_path, 'r', encoding='utf-8-sig') as f:
        reader = csv.reader(f)
        header = next(reader)

        # Find Sub_id1 column index
        try:
            sub_id1_index = header.index('Sub_id1')
        except ValueError:
            print("❌ Error: 'Sub_id1' column not found")
            return

        print(f"✅ Found Sub_id1 at column {sub_id1_index + 1}")
        print(f"\nFirst 10 rows with tracking codes:\n")

        tracking_code_counts = {}

        for i, row in enumerate(reader, start=2):
            if i <= 11:  # Show first 10 data rows
                tracking_code = row[sub_id1_index] if len(row) > sub_id1_index else ""
                print(f"Row {i}: {tracking_code}")

            # Count tracking codes
            if len(row) > sub_id1_index:
                code = row[sub_id1_index]
                tracking_code_counts[code] = tracking_code_counts.get(code, 0) + 1

        print(f"\n📊 Tracking code distribution:")
        for code, count in sorted(tracking_code_counts.items()):
            print(f"   {code}: {count} rows")

if __name__ == "__main__":
    verify_tracking_codes("AffiliateCommissionReport202510300813.csv")
