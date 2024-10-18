# brainfuck-Рефал (learner)
# Алфавит: {a,b,c,0,1,2}
# При алфавите {a,b} и языком с консультации по лабораторной пройдено верно!
import itertools


class Table:
    def __init__(self):
        self.main_part_indexes = {0}
        self.rows = []
        self.columns = []
        self.content = []
        self.alfabet = {"a", "b"}

    def add_to_main_part_index(self, index: int):
        self.main_part_indexes.add(index)

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
        print("Главная часть содержит следующие строки: ", self. main_part_indexes)
        print("Столбцы", self.columns)
        print("Строки", self.rows)
        print("Содержимое", self.content)


def make_full_table(table: Table) -> bool:
    is_full = True
    for i in range(len(table.rows)):
        must_be_in_main_part = True
        for main_len_index in table.main_part_indexes:
            if table.content[i] == table.content[main_len_index]:
                must_be_in_main_part = False
                continue
            else:
                continue
        if must_be_in_main_part:
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
            # двойным циклом пройдем все значения в пуле строк из добавленной части
            for first_string_and_pref in strings_and_prefix_pool:
                for second_string_and_pref in strings_and_prefix_pool:
                    # распаковываем кортежи
                    first_string, first_pref = first_string_and_pref
                    second_string, second_pref = second_string_and_pref
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
                                        table.add_column(remainder1+table.columns[i])
    return is_contradiction


def fill_table_from_MAT(table: Table):
    for i in range(len(table.rows)):
        for j in range(len(table.columns)):
            if table.content[i][j] is None:
                prefix = table.rows[i]
                suffix = table.columns[j]
                word = prefix + suffix
                print("Введите значение для слова", word)
                ans = input()
                table.content[i][j] = ans
                # Идем в мат за значением word и меняем table.content[i][j] на результат от МАТа


def add_counterexample(table: Table, counterexample: str):
    for i in range(1, len(counterexample) + 1):
        table.add_column(counterexample[:i])
    fill_table_from_MAT(table)


# добавление во вторичную часть новых строк
def add_new_strings(table: Table):
    for mainIndex in table.main_part_indexes:
        for letter in table.alfabet:
            if not (table.rows[mainIndex]+letter in table.rows):
                table.add_row(table.rows[mainIndex]+letter)


def send_table_to_MAT(table: Table) -> str:
    table.print()
    print("Уважаемый МАТ внесите вердикт")
    string = str(input())
    return string


if __name__ == "__main__":
    new_table = Table()

    new_table.add_column('e')
    new_table.add_row('e')

    fill_table_from_MAT(new_table)

    while True:
        add_new_strings(new_table)
        fill_table_from_MAT(new_table)

        if (make_full_table(new_table) and make_contradiction_table(new_table)):
            ok_or_counterexample = send_table_to_MAT(new_table)
            if ok_or_counterexample == "ok":
                new_table.print()
                break
            else:
                add_counterexample(new_table, ok_or_counterexample)
        else:
            continue


