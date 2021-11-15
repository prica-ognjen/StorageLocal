
import exceptions.AuthException;
import model.Connection;
import storage.Storage;
import user.Privilege;
import user.User;
import user.UserManager;
import user.UserType;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        Class.forName("model.LocalUser");

        String path = "C:\\\\Users\\\\Ognjen\\\\Desktop\\\\StorageTests";
        String sName = "test1";

        User user = UserManager.getUser("ognjen", "123", UserType.SUPER);
        user.initStorage("C:\\Users\\Ognjen\\Desktop\\StorageTests", "test1");
        Connection conn = new Connection();
        Storage s = conn.connectToStorage(path+"\\"+sName, user);
        if(s != null){
            System.out.println("\nSuccessful connection for user " + user + " to storage " + s);
        }else{
            return;
        }

        String msg = "";

        while(!msg.equals("stop")){

            System.out.print(s.getName() + ": ");

            Scanner in = new Scanner(System.in);

            msg = in.nextLine();

            if(msg.contains("addUser")){
                String[] temp = msg.split(" ");
                if(temp.length != 4){
                    System.out.println("Error: function addUser needs 3 arguments \naddUser [username] [password] [type]");
                }else{
                    System.out.println(conn.addUser(temp[1], temp[2], temp[3]));
                }
            }

            if(msg.contains("delUser")){
                String[] temp = msg.split(" ");
                if(temp.length != 2){
                    System.out.println("Error: function delUser needs 1 arguments \ndelUser [username]");
                }else{
                    System.out.println(conn.delUser(temp[1]));
                }
            }

            if(msg.contains("addPriv")){
                String[] temp = msg.split(" ");
                if(temp.length != 3){
                    System.out.println("Error: function addPriv needs 2 arguments \naddPriv [username] [privilege]");
                }else{

                    boolean ok = false;

                    for(Privilege p: Privilege.values()){
                        if(p.name().equalsIgnoreCase(temp[2])){
                            ok = true;
                            System.out.println(conn.addPrivilege(temp[1], p));
                        }
                    }
                    if(!ok)
                        System.out.println("There is no privilege with that name");

                }
            }

            if(msg.contains("delPriv")){
                String[] temp = msg.split(" ");
                if(temp.length != 3){
                    System.out.println("Error: function delPriv needs 1 arguments \ndelPriv [username] [privilege]");
                }else{
                    boolean ok = false;
                    for(Privilege p: Privilege.values()){
                        if(p.name().equalsIgnoreCase(temp[2])){
                            ok = true;
                            System.out.println(conn.delPrivilege(temp[1], p));
                        }
                    }
                    if(!ok)
                        System.out.println("There is no privilege with that name");
                }
            }

            if(msg.equals("logout")){
                conn.closeConnectionToUser();
                Storage temp = null;
                while(temp==null){
                    System.out.print(s.getName() + ": ");
                    msg = in.nextLine();
                    String[] split = msg.split(" ");

                    if(split.length != 3){
                        System.out.println("Error: to call any function you must be connected to storage.\n" +
                                "\tUse: login [username] [password] to connect to current storage.\n" +
                                "\tUse: login [pathToStorage] [username] [password] to connect to another storage.");
                    }else{
                        String username = split[1];
                        String password = split[2];
                        try {
                            temp = conn.connectToStorage(path + "\\" + sName, username, password);
                        }catch(AuthException e){
                            e.printStackTrace();
                        }
                        System.out.print("\n");
                    }


                }
            }

            if(msg.equals("stop")){
                conn.closeConnection();
                System.out.println("\nClosing the connection...");
                System.out.println("\nStopping the program...");
            }
        }


    }

}
