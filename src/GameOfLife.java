import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class GameOfLife {
    private static final int SIZE = 20;
    private static final char ALIVE_CHAR = '⬛';
    private static final char DEAD_CHAR = '⬜';

    // История всех состояний поля для навигации назад/вперед
    private static final List<boolean[][]> history = new ArrayList<>();
    // Индекс текущего просматриваемого поколения в истории
    private static int currentStepIndex = -1;
    // Флаг, определяющий, находится ли игра в режиме симуляции новых шагов
    private static boolean isGameOver = false;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean[][] initialGrid = new boolean[SIZE][SIZE];

        System.out.println("=== Игра Конвея \"Жизнь\" ===");
        System.out.println("Введите начальные координаты живых клеток в формате: строка колонка (индексы от 0 до 19).");
        System.out.println("Пример: создание глайдера - [0 1] [1 2] [2 0] [2 1] [2 2]");
        System.out.println("Для завершения ввода введите 'start' или пустую строку.");

        // Ввод начального состояния
        while (true) {
            System.out.print("Клетка (строка столбец): ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("start") || input.isEmpty()) {
                break;
            }
            String[] parts = input.split("\\s+");
            if (parts.length == 2) {
                try {
                    int r = Integer.parseInt(parts[0]);
                    int c = Integer.parseInt(parts[1]);
                    if (r >= 0 && r < SIZE && c >= 0 && c < SIZE) {
                        initialGrid[r][c] = true;
                        System.out.println("Клетка [" + r + "][" + c + "] добавлены.");
                    } else {
                        System.out.println("Ошибка: Индексы должны быть от 0 до " + (SIZE - 1));
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка: Введите два целых числа через пробел.");
                }
            } else {
                System.out.println("Ошибка: Неверный формат ввода.");
            }
        }

        // Сохраняем начальное поколение
        history.add(initialGrid);
        currentStepIndex = 0;

        // Основной цикл управления
        while (true) {
            printGrid(history.get(currentStepIndex));
            printMenu();

            String command = scanner.nextLine().trim().toLowerCase();

            if (command.equals("q")) {
                System.out.println("Выход из игры.");
                break;
            } else if (command.equals("b")) { // Назад
                if (currentStepIndex > 0) {
                    currentStepIndex--;
                } else {
                    System.out.println("Это самое первое поколение! Назад нельзя.");
                }
            } else if (command.equals("f")) { // Вперед по истории
                if (currentStepIndex < history.size() - 1) {
                    currentStepIndex++;
                } else {
                    System.out.println("Вы дошли до конца вычисленной истории. Используйте Enter для генерации нового шага.");
                }
            } else { // Любая другая клавиша (например, Enter) — расчет следующего поколения
                if (isGameOver) {
                    if (currentStepIndex < history.size() - 1) {
                        currentStepIndex++;
                    } else {
                        System.out.println("Игра завершена по правилам остановки. Новые поколения невозможны.");
                    }
                } else {
                    // Если пользователь вернулся назад по истории и нажал Enter,
                    // симулируем шаг от самого последнего вычисленного состояния
                    if (currentStepIndex < history.size() - 1) {
                        currentStepIndex = history.size() - 1;
                        System.out.println("Перемотка к последнему актуальному поколению...");
                        continue;
                    }

                    boolean[][] nextGrid = computeNextGeneration(history.get(currentStepIndex));

                    // Проверка правил остановки
                    if (isAllDead(nextGrid)) {
                        System.out.println("\n[Стоп]: На поле не осталось ни одной живой клетки (Правило 6).");
                        isGameOver = true;
                    } else if (isIdentical(history.get(currentStepIndex), nextGrid)) {
                        System.out.println("\n[Стоп]: Конфигурация клеток не изменилась (Правило 7).");
                        isGameOver = true;
                    } else {
                        int repeatIndex = findRepeat(nextGrid);
                        if (repeatIndex != -1) {
                            System.out.println("\n[Стоп]: Конфигурация в точности повторила поколение №" + repeatIndex + " (Правило 8).");
                            isGameOver = true;
                        }
                    }

                    // Добавляем новое поколение в историю в любом случае, чтобы зафиксировать финальный шаг
                    history.add(nextGrid);
                    currentStepIndex++;
                }
            }
        }
        scanner.close();
    }

    // Вывод игрового поля на экран
    private static void printGrid(boolean[][] grid) {
        System.out.println("\nПоколение №" + currentStepIndex);
        // Вывод верхней границы поля
        System.out.print("   ");
        for (int c = 0; c < SIZE; c++) System.out.printf("%2d", c);
        System.out.println();

        for (int r = 0; r < SIZE; r++) {
            System.out.printf("%2d |", r);
            for (int c = 0; c < SIZE; c++) {
                System.out.print((grid[r][c] ? ALIVE_CHAR : DEAD_CHAR) + " ");
            }
            System.out.println("|");
        }
    }

    // Вывод доступных команд
    private static void printMenu() {
        System.out.print("[Enter] - След. шаг | [B] - Назад | [F] - Вперед | [Q] - Выход: ");
    }

    // Расчет следующего поколения с учетом зацикленности поля (Тор)
    private static boolean[][] computeNextGeneration(boolean[][] current) {
        boolean[][] next = new boolean[SIZE][SIZE];

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int aliveNeighbors = countAliveNeighbors(current, r, c);

                if (current[r][c]) {
                    // Правило 4: Живая клетка остается живой, если у нее 2 или 3 соседа
                    next[r][c] = (aliveNeighbors == 2 || aliveNeighbors == 3);
                } else {
                    // Правило 5: Мертвая клетка оживает, если у нее ровно 3 соседа
                    next[r][c] = (aliveNeighbors == 3);
                }
            }
        }
        return next;
    }

    // Подсчет соседей с учетом «зацикливания» по вертикали и горизонтали
    private static int countAliveNeighbors(boolean[][] grid, int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;

                // Зацикливание индексов через остаток от деления
                int neighborRow = (row + dr + SIZE) % SIZE;
                int neighborCol = (col + dc + SIZE) % SIZE;

                if (grid[neighborRow][neighborCol]) {
                    count++;
                }
            }
        }
        return count;
    }

    // Правило 6: Проверка, что все клетки мертвы
    private static boolean isAllDead(boolean[][] grid) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c]) return false;
            }
        }
        return true;
    }

    // Правило 7: Сравнение двух матриц на идентичность
    private static boolean isIdentical(boolean[][] grid1, boolean[][] grid2) {
        for (int r = 0; r < SIZE; r++) {
            if (!Arrays.equals(grid1[r], grid2[r])) {
                return false;
            }
        }
        return true;
    }

    // Правило 8: Поиск повторения конфигурации в истории поколений
    private static int findRepeat(boolean[][] grid) {
        for (int i = 0; i < history.size(); i++) {
            if (isIdentical(history.get(i), grid)) {
                return i;
            }
        }
        return -1;
    }
}