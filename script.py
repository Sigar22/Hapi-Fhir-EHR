import csv

input_file = 'input.csv'
output_file = 'LOINC.csv'
keep_columns = ['code', 'units']  # Названия столбцов которые нужно сохранить

with open(input_file, mode='r', encoding='utf-8') as infile, \
     open(output_file, mode='w', encoding='utf-8', newline='') as outfile:

    reader = csv.DictReader(infile)
    writer = csv.DictWriter(outfile, fieldnames=keep_columns)
    
    writer.writeheader()
    
    for row in reader:
        # Создаем новую строку только с нужными столбцами
        filtered_row = {col: row[col] for col in keep_columns}
        writer.writerow(filtered_row)