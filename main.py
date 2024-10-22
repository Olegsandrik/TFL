# brainfuck-Рефал (learner)
# Алфавит: {a,b,c,0,1,2}
# При алфавите {a,b} и языком с консультации по лабораторной пройдено верно!
import itertools
import requests
import json


class Table:
    def __init__(self):
        self.main_part_indexes = [0]
        self.rows = []
        self.columns = []
        self.content = []
        self.alfabet = {"L", "R"}

    def add_to_main_part_index(self, index: int):
        self.main_part_indexes.append(index)

    def add_column(self, column: str):
        self.columns.append(column)
        for i in range(len(self.rows)):
            self.content[i].append(None)

    def add_row(self, row: str):
        self.rows.append(row)
        new = []
        for i in range(len(self.columns)):
            new.append(None)
        self.content.append(new)

    def add_content(self, row_index: int, column_index: int, content: bool):
        self.content[row_index][column_index] = content

    def print(self):
        print("Главная часть содержит следующие строки: ", self.main_part_indexes)
        print("Столбцы", self.columns)
        print("Строки", self.rows)
        print("Содержимое", self.content)


def make_full_table(table: Table) -> bool:
    is_full = True
    for i in range(len(table.rows)):
        if i in table.main_part_indexes:
            continue

        in_main_part = False
        for index in table.main_part_indexes:
            if table.content[i] == table.content[index]:
                in_main_part = True
                break

        if not in_main_part:
            is_full = False
            table.add_to_main_part_index(i)

    return is_full


# данная функция, на мой взгляд, наименее понятна для чтения, поэтому добавлю комментарии
def make_contradiction_table(table: Table) -> bool:
    is_contradiction = True
    # Генерируем все возможные пары индексов из главной части
    pairs = list(itertools.combinations(table.main_part_indexes, 2))
    eq_pairs = []
    for pair in pairs:
        # распаковываем кортеж индексов
        first_string_index, second_string_index = pair
        # если совпали строчки по индексам, то добавляем в пул eq_pairs соответсвующие префиксы
        if table.content[first_string_index] == table.content[second_string_index]:
            eq_pairs.append((table.rows[first_string_index], table.rows[second_string_index]))
    if len(eq_pairs) == 0:
        # пар с одинаковыми значениями не нашлось => выходим
        return is_contradiction
    else:
        # если нет, то надо найти все пары не из главной части
        strings_and_prefix_pool = []
        for index in range(len(table.rows)):
            if index not in table.main_part_indexes:
                # взяли строчки не в главной части и их префиксы
                strings_and_prefix_pool.append((table.content[index], table.rows[index]))

        # теперь берем пары префиксов
        for pair_strings in eq_pairs:
            # распаковываем кортеж
            main_first_string_pref, main_second_string_pref = pair_strings

            for i in range(len(strings_and_prefix_pool)):
                for j in range(i + 1, len(strings_and_prefix_pool)):
                    first_string, first_pref = strings_and_prefix_pool[i][0], strings_and_prefix_pool[i][1]
                    second_string, second_pref = strings_and_prefix_pool[j][0], strings_and_prefix_pool[j][1]
                    # Во избежании совпадения во время прохода двойным циклом
                    if first_pref != second_pref:
                        # условие на вхождение суффикса главной части в суффикс добавленной части (подстрока в начале)
                        if (first_pref.startswith(main_first_string_pref)
                                and second_pref.startswith(main_second_string_pref)):
                            remainder1 = first_pref[len(main_first_string_pref):]
                            remainder2 = second_pref[len(main_second_string_pref):]
                            # сравниваем gamma и и если вдруг строки не совпали, то мы нашли то, что искали
                            if remainder1 == remainder2 and first_string != second_string:
                                is_contradiction = False
                                # найдем v^k
                                for i in range(len(first_string)):
                                    if first_string[i] != second_string[i]:
                                        table.add_column(remainder1 + table.columns[i])
    return is_contradiction


def fill_table_from_MAT(table: Table):
    for i in range(len(table.rows)):
        for j in range(len(table.columns)):
            if table.content[i][j] is None:
                prefix = table.rows[i]
                suffix = table.columns[j]
                word = prefix + suffix
                url = 'http://0.0.0.0:8095/checkWord'

                if word[0] == 'ε':
                    word = word[1:]

                data = {
                    'word': word
                }

                response = requests.post(url, json=data)
                json_response = response.json()
                ans = int(json_response.get('response'))
                print("Добавили значение:", word, ans)
                table.content[i][j] = ans


def add_counterexample(table: Table, counterexample: str):
    counterexample = counterexample[::-1]
    for i in range(1, len(counterexample) + 1):
        add = counterexample[:i]
        table.add_column(add[::-1])
    fill_table_from_MAT(table)


# добавление во вторичную часть новых строк
def add_new_strings(table: Table):
    for mainIndex in table.main_part_indexes:
        for letter in table.alfabet:
            if not (table.rows[mainIndex] + letter in table.rows):
                table.add_row(table.rows[mainIndex] + letter)


def send_table_to_MAT(table: Table) -> str:
    url = 'http://0.0.0.0:8095/checkTable'

    suffixes = ' '.join(table.columns)
    main_prefixes = []
    non_main_prefixes = []
    table_main = []
    table_non_main = []
    print("-------------------")
    table.print()

    print("len rows: ", len(table.rows))
    for index_row in range(len(table.rows)):
        if index_row in table.main_part_indexes:
            main_prefixes.append(table.rows[index_row])
            table_main.append(table.content[index_row])
        else:
            non_main_prefixes.append(table.rows[index_row])
            table_non_main.append(table.content[index_row])

    # table_data ' '.join(map(str, itertools.chain.from_iterable(table_main)))
    main_prefixes_res = ' '.join(main_prefixes)
    non_main_prefixes_res = ' '.join(non_main_prefixes)

    table_main_res = ' '.join(map(str, itertools.chain.from_iterable(table_main)))
    table_non_main_res = ' '.join(map(str, itertools.chain.from_iterable(table_non_main)))

    data = {
        "main_prefixes": main_prefixes_res,
        "non_main_prefixes": non_main_prefixes_res,
        "suffixes": suffixes,
        "table": table_main_res +' '+ table_non_main_res,
    }

    response = requests.post(url, json=data)
    json_response = response.json()
    ans = bool(json_response.get('type'))
    counterexample = str(json_response.get('response'))
    print("data:", data)
    print("ans:", json_response)
    print("-------------------")
    if ans is True or ans is False:
        return counterexample

    if ans is None:
        return "ok"


if __name__ == "__main__":
    new_table = Table()

    new_table.add_column('ε')
    new_table.add_row('ε')

    fill_table_from_MAT(new_table)

    while True:
        add_new_strings(new_table)
        fill_table_from_MAT(new_table)

        if make_full_table(new_table):
            if make_contradiction_table(new_table):
                ok_or_counterexample = send_table_to_MAT(new_table)
                if ok_or_counterexample == "ok":
                    new_table.print()
                    break
                else:
                    add_counterexample(new_table, ok_or_counterexample)
            else:
                new_table.print()
                continue
        else:
            new_table.print()
            continue
