package dev.infend.skyrimtext;
/** Custom story NPCs, excluded from the original-game catalog. */
final class AuthorNpcProfiles {
    interface Registrar { void add(String name,String race,String gender,String role,int variant,String... aliases); }
    static void register(Registrar add){
        add.add("Ливия","Имперец","Женский","Имперский капитан Хелгена",1,"Имперский капитан","Имперский капитан Хелгена","Капитан Хелгена");
        add.add("Альма","Бретон","Женский","Алая Бессмертная | маг крови",1);
        add.add("Кэтрин","Человек","Женский","Носитель Нулевого Предела",1);
        add.add("Итачи","Человек","Женский","Странница | мастер иллюзий и духовных техник",1,"Итачи Учиха");
    }
    private AuthorNpcProfiles(){}
}
