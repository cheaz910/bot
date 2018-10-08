package bot;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.gson.Gson;

class Bot {
    private Scanner in;

    public Bot(PrintStream outputStream, InputStream inputStream) {
        System.setOut(outputStream);
        in = new Scanner(inputStream);
    }

    void Start() {
        String nameOfUser = GetName();

        Map<String, Map<String, Log>> log = ConvertToMap(FileWorker.ReadFile("Log.txt"));
        if (log == null)
            log = new HashMap<>();
        if (!log.containsKey(nameOfUser))
            log.put(nameOfUser, new HashMap());

        System.out.println(GetGreeting(nameOfUser));
        System.out.println(GetHelp());

        boolean isCommand = true;
        while (isCommand) {
            switch(in.nextLine()) {
                case "+":
                    System.out.println("Введите через пробел событие, дату начала и продолжительность события. " +
                            "Формат ввода: событие HH:mm-dd.MM.yyyy HH:mm");
                    AddNote(in.nextLine().split(" "), log.get(nameOfUser));
                    break;
                case "-":
                    System.out.println("Введите дату начала события. " +
                            "Формат ввода: HH:mm-dd.MM.yyyy");
                    RemoveNote(in.nextLine(), log.get(nameOfUser));
                    break;
                case "перенести":
                    System.out.println("Введите сначала дату, с которой нужно перенести, " +
                            "затем дату, на которую нужно перенести. " +
                            "Формат ввода: HH:mm-dd.MM.yyyy HH:mm-dd.MM.yyyy");
                    TransferNote(in.nextLine().split(" "), log.get(nameOfUser));
                    break;
                case "день":
                    System.out.println("Ведите интересующий вас день. " +
                            "Формат ввода: dd.MM.yyyy");
                    ArrayList<Log> notesForDay = GetNotes(in.nextLine(), log.get(nameOfUser), "dd.MM.yyyy");
                    DisplayListOfNotes(notesForDay);
                    break;
                case "месяц":
                    System.out.println("Введите интересующий вас месяц. " +
                            "Формат ввода: MM.yyyy");
                    ArrayList<Log> notesForMonth = GetNotes(in.nextLine(), log.get(nameOfUser), "MM.yyyy");
                    DisplayListOfNotes(notesForMonth);
                    break;
                case "спасибо":
                    isCommand = false;
                    break;
                case "справка":
                    System.out.println(GetHelp());
                    break;
                default:
                    System.out.println("Неизвестная команда");
                    break;
            }
        }
        FileWorker.WriteFile("Log.txt", ConvertToJson(log));
    }

    private String GetName() {
        System.out.println("Как вас зовут?");
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    String GetGreeting(String name) {
        return String.format("Здравствуй, %s!", name);
    }

    private String GetHelp() {
        return "Чтобы добавить событие, введите: +. \n" +
                "Чтобы удалить событие, введите: -. \n" +
                "Чтобы перенести событие, введите: перенести. \n" +
                "Чтобы посмотреть все события за день, введите: день. \n" +
                "Чтобы посмотреть все события за месяц, введите: месяц. \n" +
                "Чтобы завершить работу, введите: спасибо. ";
    }

    void AddNote(String[] info, Map<String, Log> notes) {
        if (info.length != 3){
            System.out.println("Неверный формат ввода");
            return;
        }
        String strStartDate = info[1];
        Date startDate = GetCorrectDate(strStartDate, "HH:mm-dd.MM.yyyy");
        Date duration = GetCorrectDate(info[2], "HH:mm");
        if (startDate == null || duration == null) {
            return;
        }
        Date endDate = GetEndDate(startDate, duration);
        Log newLog = new Log(info[0], startDate, endDate);
        if (IsConflict(notes, newLog)) {
            System.out.println("На это время уже запланировано событие");
            return;
        }
        notes.put(GetCorrectStringFromDate(startDate, "HH:mm-dd.MM.yyyy"), newLog);
        System.out.println("Событие добавлено");
    }

    Date GetEndDate(Date startDate, Date duration) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.MINUTE, duration.getHours() * 60 + duration.getMinutes());
        return cal.getTime();
    }

    private boolean IsConflict(Map<String, Log> notes, Log newLog) {
        for (Log log : notes.values()) {
            if (DoNotesIntersect(log, newLog)){
                return true;
            }
        }
        return false;
    }

    private boolean DoNotesIntersect(Log first, Log second) {
        return !((first.startDate.after(second.startDate) && first.endDate.after(second.endDate) &&
                  first.startDate.after(second.endDate)) ||
                (first.startDate.before(second.startDate) && first.endDate.before(second.endDate) &&
                         second.startDate.after(first.endDate)));
    }

    void RemoveNote(String strDate, Map<String, Log> notes) {
        Date date = GetCorrectDate(strDate, "HH:mm-dd.MM.yyyy");
        if (date == null) {
            System.out.println("Неверный формат ввода");
            return;
        }
        if (!notes.containsKey(strDate)) {
            System.out.println("Такого события нет");
            return;
        }
        notes.remove(strDate);
        System.out.println("Событие удалено");
    }

    void TransferNote(String[] info, Map<String, Log> notes) {
        if (info.length != 2){
            System.out.println("Неверный формат ввода");
            return;
        }
        String strOldDate = info[0];
        String strNewDate = info[1];
        Date oldDate = GetCorrectDate(strOldDate, "HH:mm-dd.MM.yyyy");
        Date newDate = GetCorrectDate(strNewDate, "HH:mm-dd.MM.yyyy");
        if (oldDate == null || newDate == null) {
            return;
        }
        String note = notes.get(strOldDate).note;
        Date endDate = RecalculateEndDate(oldDate, notes.get(strOldDate).endDate, newDate);
        Log newLog = new Log(note, newDate, endDate);
        if (IsConflict(notes, newLog)) {
            System.out.println("На это время уже запланировано событие");
            return;
        }
        notes.remove(strOldDate);
        notes.put(strNewDate, newLog);
        System.out.println("Событие перенесено");
    }

    Date RecalculateEndDate(Date startOldDate, Date endOldDate, Date startNewDate) {
        int diffInMinutes = (int)((endOldDate.getTime() - startOldDate.getTime()) / (1000 * 60));
        Calendar cal = Calendar.getInstance();
        cal.setTime(startNewDate);
        cal.add(Calendar.MINUTE, diffInMinutes);
        return cal.getTime();
    }

    ArrayList<Log> GetNotes(String strMonthOrDay, Map<String, Log> log, String pattern) {
        Date needDate = GetCorrectDate(strMonthOrDay, pattern);
        if (needDate == null) {
            return new ArrayList<>();
        }
        ArrayList<Log> result = new ArrayList<>();
        for (String date : log.keySet()) {
            Date currentDate = log.get(date).startDate;
            if (needDate.getYear() == currentDate.getYear() &&
                    needDate.getMonth() == currentDate.getMonth() &&
                    (needDate.getDate() == currentDate.getDate() || pattern.equals("MM.yyyy"))) {
                result.add(log.get(date));
            }
        }
        return result;
    }

    private void DisplayListOfNotes(ArrayList<Log> notes) {
        String pattern = "HH:mm-dd.MM.yyyy";
        for (Log log : notes) {
            System.out.println("Начало события: " + GetCorrectStringFromDate(log.startDate, pattern) +
                    " Конец события: " + GetCorrectStringFromDate(log.endDate, pattern) +
                    " Cобытие: " + log.note);
        }
    }

    Date GetCorrectDate(String strDate, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
        Date date;
        try {
            date = format.parse(strDate);
        }
        catch (ParseException e) {
            System.out.println("Неверный формат даты/времени. [" + strDate + "]");
            return null;
        }
        return date;
    }

    private String GetCorrectStringFromDate(Date date, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
        return format.format(date);
    }

    String ConvertToJson(Map<String, Map<String, Log>> log) {
        return new Gson().toJson(log, HashMap.class);
    }

    HashMap<String, Map<String, Log>> ConvertToMap(String log) {
        return new Gson().fromJson(log, MyMap.class);
    }
}
