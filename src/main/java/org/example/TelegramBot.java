package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TelegramBot  extends TelegramLongPollingBot {
    private Map<Long,String> chatIds;
    private HashMap<Long,Integer> mostUserP;
    private List<String> api;
    private ApiManager apiManager;
    private HashMap<String,Integer> counterMap;
    private List<String> historyActivities;
    private Thread chartThread;
    private InteractionChart chart;
    private Panel panel;

    public TelegramBot(List<String> api,Panel panel){
        this.chatIds = new HashMap<>();
        this.api = api;
        this.apiManager = new ApiManager();
        createCounterMap();
        this.chart = new InteractionChart(this);
        this.chartThread = new Thread(this.chart);
        this.chartThread.start();
        this.mostUserP = new HashMap<>();
        this.panel = panel;
        update();
        this.historyActivities =new ArrayList<>();
    }

    private void createCounterMap() {
        this.counterMap = new HashMap<>();
        this.counterMap.put("Massage",0);
        this.counterMap.put(Constants.API_CAT_FACT,0);
        this.counterMap.put(Constants.API_JOKES,0);
        this.counterMap.put(Constants.API_ACTIVITIES,0);
        this.counterMap.put(Constants.API_NUMBERS,0);
        this.counterMap.put(Constants.API_RANDOM_DOG,0);
    }

    public void updateApiList(List<String> api){
        this.api = api;
    }
    @Override
    public String getBotUsername() {
        return "AYRD_Telegram_Bot";
    }

    @Override
    public String getBotToken() {
        return "6210516288:AAFlfGhiWl-Q6z4KADzVToIdLhKK_H7qQqM";
    }

    @Override
    public void onUpdateReceived(Update update) {
        int counter ;
        SendMessage sendMessage= new SendMessage();
        SendPhoto sendPhoto = new SendPhoto();
        long chatId= getChatID(update);
        sendMessage.setChatId(chatId);
        sendPhoto.setChatId(chatId);
        long timestamp ;
        ZonedDateTime date = null;
        Message message = update.getMessage();
        CallbackQuery callbackQuery = update.getCallbackQuery();

        if (this.chatIds.containsKey(chatId)){
            int count = this.mostUserP.get(chatId);
            count++;
            this.mostUserP.put(chatId,count);
        }else {
            this.mostUserP.put(chatId,1);
        }
        if (update.getMessage()!=null){
            timestamp = message.getDate();
            counter = this.counterMap.get("Massage")+1;
            this.counterMap.put("Massage",counter);
            this.chatIds.put(chatId,message.getFrom().getFirstName());
            System.out.println(this.chatIds);
            sendMessage.setText("Choose Api: ");

            List<InlineKeyboardButton> buttons = IntStream.range(0, this.api.size())
                    .mapToObj(i -> {
                        InlineKeyboardButton button = new InlineKeyboardButton(this.api.get(i));
                        button.setCallbackData(this.api.get(i));
                        return button;
                    })
                    .collect(Collectors.toList());

            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(buttons);
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            inlineKeyboardMarkup.setKeyboard(keyboard);

            sendMessage.setReplyMarkup(inlineKeyboardMarkup);
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);
        }else {
            timestamp=callbackQuery.getMessage().getDate();
            if(update.getCallbackQuery().getData().equals(Constants.API_CAT_FACT)){
                counter = this.counterMap.get(Constants.API_CAT_FACT)+1;
                this.counterMap.put(Constants.API_CAT_FACT,counter);
                sendMessage.setText(this.apiManager.catsFactApi());
            } else if (update.getCallbackQuery().getData().equals(Constants.API_JOKES)){
                counter = this.counterMap.get(Constants.API_JOKES)+1;
                this.counterMap.put(Constants.API_JOKES,counter);
                sendMessage.setText(this.apiManager.jokeApi("Any"));
            } else if (update.getCallbackQuery().getData().equals(Constants.API_ACTIVITIES)){
                counter = this.counterMap.get(Constants.API_ACTIVITIES)+1;
                this.counterMap.put(Constants.API_ACTIVITIES,counter);
                sendMessage.setText(this.apiManager.activitiesApi());
            }else if (update.getCallbackQuery().getData().equals(Constants.API_RANDOM_DOG)){
                counter = this.counterMap.get(Constants.API_RANDOM_DOG)+1;
                this.counterMap.put(Constants.API_RANDOM_DOG,counter);
                this.apiManager.dogApi();
                File file = new File("res/dog/randomDog.jpg");
                System.out.println(file.getName());
                InputFile randomAhhDog = new InputFile(file);
                sendPhoto.setPhoto(randomAhhDog);
            }
            else{
                counter = this.counterMap.get(Constants.API_NUMBERS)+1;
                this.counterMap.put(Constants.API_NUMBERS,counter);
                sendMessage.setText(this.apiManager.numberApi());
            }
            if (update.hasCallbackQuery()){
                ZonedDateTime currentTime = ZonedDateTime.now();
                date = currentTime;
            }
        }
        try {
            execute(sendMessage);

        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        finally {

            assert date != null;
            getRecentInteractions("Name: "+this.chatIds.get(chatId)+" API Used: "+update.getCallbackQuery().getData()+ "\nDate: "+ date.format(DateTimeFormatter.ISO_LOCAL_DATE) + " /" + date.format(DateTimeFormatter.ISO_LOCAL_TIME));

            try {
                execute(sendPhoto);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            //System.out.println(sendPhoto);
        }


    }



    public long getChatID(Update update){
        long update1=0;
        if (update.getMessage()!=null){
            update1=update.getMessage().getChatId();
        }else {
            update1=update.getCallbackQuery().getMessage().getChatId();
        }
        return update1;
    }
    public void update(){
        new Thread(()->{
            while (true){
                this.panel.setTotalRequestsNumberText(String.valueOf(this.counterMap.values().stream().reduce(Integer::sum).orElse(0)));
                this.panel.setMostPopularActivityName(set());
                this.panel.setTotalUsersNumberText(String.valueOf(this.chatIds.size()));
                this.panel.setMostActiveUserNameText(this.chatIds.get(get()));
                this.panel.setTextHistoryArea(getHistoryText());
            }

        }).start();
    }
    private  Long get(){
        return new HashMap<>(this.mostUserP).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0L);
    }
    private String set(){
        if(this.counterMap.values().stream().reduce(Integer::sum).orElse(0)==0){
            return  "No activity found";
        } else {
            return counterMap.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("No activity found");
        }
    }
    public void getRecentInteractions(String info) {
        if (this.historyActivities.size() >= 10) {
            this.historyActivities.remove(0);
        }
        this.historyActivities.add(info);
    }


    private String getHistoryText(){
        String text="";
        int index = 1;
        for (String i: this.historyActivities ) {
            text+=index+") "+i+"\n\n";
            index++;

        }
        return text;
    }


    public void initialUsers() {
      //  this.chatIds.clear();
    }

    public HashMap<String, Integer> getCounterMap() {
        return counterMap;
    }
}
//                InlineKeyboardButton programming= new InlineKeyboardButton("Programming");
//                programming.setCallbackData("Programming");
//                InlineKeyboardButton misc = new InlineKeyboardButton("Misc");
//                misc.setCallbackData("Misc");
//                InlineKeyboardButton dark= new InlineKeyboardButton("Dark");
//                dark.setCallbackData("Dark");
//                InlineKeyboardButton pun= new InlineKeyboardButton("Pun");
//                pun.setCallbackData("Pun");
//                InlineKeyboardButton spooky = new InlineKeyboardButton("Spooky");
//                spooky.setCallbackData("Spooky");
//                InlineKeyboardButton christmas = new InlineKeyboardButton("Christmas");
//                christmas.setCallbackData("Christmas");
//                InlineKeyboardButton any = new InlineKeyboardButton("Any");
//                any.setCallbackData("Any");
//                List<InlineKeyboardButton> topRow = Arrays.asList(programming,misc, dark,pun,spooky,christmas,any);
//                List<List<InlineKeyboardButton>> keyboard= Arrays.asList(topRow);
//                InlineKeyboardMarkup inlineKeyboardMarkup= new InlineKeyboardMarkup();
//                inlineKeyboardMarkup.setKeyboard(keyboard);
//                sendMessage.setReplyMarkup(inlineKeyboardMarkup);