package model.miniGame;

import model.enums.PlantType;
import model.enums.VaseContentType;
import model.enums.ZombieType;
import java.util.List;

public final class VasebreakerLevels {
    private VasebreakerLevels() { }
    public static VasebreakerLevel level(int level) {
        return switch (level) {
            case 1 -> new VasebreakerLevel(1, 80, List.of(
                    normal("v1",0,4,VaseContentType.EMPTY,null,null),
                    normal("v2",1,4,VaseContentType.ZOMBIE,null,ZombieType.NORMAL),
                    normal("v3",2,4,VaseContentType.PLANT_PACKET,PlantType.PEASHOOTER,null),
                    plant("v4",3,4,PlantType.WALL_NUT),
                    normal("v5",4,4,VaseContentType.ZOMBIE,null,ZombieType.CONEHEAD)));
            case 2 -> new VasebreakerLevel(2, 60, List.of(
                    normal("v1",0,3,VaseContentType.ZOMBIE,null,ZombieType.NORMAL),
                    normal("v2",1,3,VaseContentType.PLANT_PACKET,PlantType.SNOW_PEA,null),
                    plant("v3",2,3,PlantType.REPEATER),
                    normal("v4",3,3,VaseContentType.ZOMBIE,null,ZombieType.BUCKETHEAD),
                    normal("v5",4,3,VaseContentType.EMPTY,null,null),
                    normal("v6",0,5,VaseContentType.ZOMBIE,null,ZombieType.CONEHEAD),
                    normal("v7",2,5,VaseContentType.PLANT_PACKET,PlantType.WALL_NUT,null),
                    normal("v8",4,5,VaseContentType.ZOMBIE,null,ZombieType.NORMAL)));
            case 3 -> new VasebreakerLevel(3, 40, List.of(
                    normal("v1",0,2,VaseContentType.ZOMBIE,null,ZombieType.BUCKETHEAD),
                    normal("v2",1,2,VaseContentType.PLANT_PACKET,PlantType.REPEATER,null),
                    plant("v3",2,2,PlantType.SNOW_PEA),
                    normal("v4",3,2,VaseContentType.ZOMBIE,null,ZombieType.CONEHEAD),
                    normal("v5",4,2,VaseContentType.EMPTY,null,null),
                    normal("v6",0,4,VaseContentType.ZOMBIE,null,ZombieType.KNIGHT),
                    normal("v7",1,4,VaseContentType.PLANT_PACKET,PlantType.WALL_NUT,null),
                    garg("v8",2,4),
                    normal("v9",3,4,VaseContentType.ZOMBIE,null,ZombieType.BUCKETHEAD),
                    plant("v10",4,4,PlantType.MELON_PULT),
                    normal("v11",1,6,VaseContentType.ZOMBIE,null,ZombieType.NORMAL),
                    normal("v12",3,6,VaseContentType.PLANT_PACKET,PlantType.CHERRY_BOMB,null)));
            default -> throw new IllegalArgumentException("Vasebreaker level must be 1, 2, or 3.");
        };
    }
    private static Vase normal(String id,int r,int c,VaseContentType content,PlantType p,ZombieType z){return new Vase(id,r,c,VaseType.NORMAL,content,p,z);}
    private static Vase plant(String id,int r,int c,PlantType p){return new Vase(id,r,c,VaseType.PLANT,VaseContentType.PLANT_PACKET,p,null);}
    private static Vase garg(String id,int r,int c){return new Vase(id,r,c,VaseType.GARGANTUAR,VaseContentType.GARGANTUAR,null,ZombieType.GARGANTUAR);}
}
