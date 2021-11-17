
import exceptions.AuthException;
import model.Connection;
import storage.Storage;
import user.*;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        Class.forName("model.LocalUser");

        String path = "C:\\\\Users\\\\Ognjen\\\\Desktop\\\\StorageTests";
        String sName = "test1";

        User user = UserManager.getUser("pavle", "123", UserType.SUPER);
        //user.initStorage("C:\\Users\\Ognjen\\Desktop\\StorageTests", "test1");
        Connection conn = new Connection();
        Storage s = conn.connect(path+"\\"+sName, user);
        if(s != null){
            System.out.println("\nSuccessful connection for user " + user + " to storage " + s);
        }else{
            return;
        }

        String msg = "";

        String currDir = s.getName();

        while(!msg.equals("stop")){

            System.out.print(currDir + ": ");

            Scanner in = new Scanner(System.in);

            msg = in.nextLine();

            String[] splitMsg = msg.split(" ");

            if(splitMsg[0].equals("addUser")){
                String[] temp = msg.split(" ");
                if(temp.length != 4){
                    System.out.println("Error: function addUser needs 3 arguments \naddUser [username] [password] [type]");
                }else{
                    conn.addUser(temp[1], temp[2], UserType.REGULAR);
                }
            }

            if(splitMsg[0].equals("delUser")){
                String[] temp = msg.split(" ");
                if(temp.length != 2){
                    System.out.println("Error: function delUser needs 1 arguments \ndelUser [username]");
                }else{
                    conn.delUser(temp[1]);
                }
            }

            if(splitMsg[0].equals("addPriv")){
                String[] temp = msg.split(" ");
                if(temp.length != 3 && temp.length != 4){
                    System.out.println("Error: function addPriv needs 2 arguments \naddPriv [username] [privilege]");
                }else{
                    if(temp.length == 3){
                        boolean ok = false;
                        for(Privilege p: Privilege.values()){
                            if(p.name().equalsIgnoreCase(temp[2])){
                                ok = true;
                                conn.addPrivilege(temp[1], p);
                            }
                        }
                        if(!ok)
                            System.out.println("There is no privilege with that name");
                    }else{
                        boolean ok = false;
                        for(Privilege p: Privilege.values()){
                            if(p.name().equalsIgnoreCase(temp[2])){
                                ok = true;
                                conn.addPrivilege(temp[1], p, temp[3]);
                            }
                        }
                        if(!ok)
                            System.out.println("There is no privilege with that name");
                    }

                }
            }

            if(splitMsg[0].equals("delPriv")){
                String[] temp = msg.split(" ");
                if(temp.length != 3 && temp.length != 4){
                    System.out.println("Error: function delPriv needs 1 arguments \ndelPriv [username] [privilege]\ndelPriv [username] [privilege] [path]");
                }else{
                    if(temp.length == 3){
                        boolean ok = false;
                        for(Privilege p: Privilege.values()){
                            if(p.name().equalsIgnoreCase(temp[2])){
                                ok = true;
                                conn.delPrivilege(temp[1], p);
                            }
                        }
                        if(!ok)
                            System.out.println("There is no privilege with that name");
                    }else{
                        boolean ok = false;
                        for(Privilege p: Privilege.values()){
                            if(p.name().equalsIgnoreCase(temp[2])){
                                ok = true;
                                conn.delPrivilege(temp[1], p, temp[3]);
                            }
                        }
                        if(!ok)
                            System.out.println("There is no privilege with that name");
                    }


                }
            }

            if(splitMsg[0].equals("addFile")){
                String[] temp = msg.split(" ");
                if(temp.length != 3 && temp.length != 4){
                    System.out.println("Error: function addFile needs 2 or 3 arguments \naddFile [fileName] [type[f/d]]\naddFile [fileName] [pathFromRoot] [type[f/d]]");
                }else{
                    if(temp.length==3){
                        if(temp[2].equals("f"))
                            conn.mk(temp[1], FileType.FILE);
                        else if(temp[2].equals("d"))
                            conn.mk(temp[1], FileType.DIR);
                    }
                    else{
                        if(temp[3].equals("f"))
                            conn.mk(temp[1], temp[2], FileType.FILE);
                        else if(temp[3].equals("d"))
                            conn.mk(temp[1], temp[2], FileType.DIR);
                    }

                }
            }
            if(splitMsg[0].equals("delFile")){
                String[] temp = msg.split(" ");
                if(temp.length != 2 && temp.length != 3){
                    System.out.println("Error: function delFile needs 1 or 2 arguments \ndelFile [fileName]\ndelFile [fileName] [pathFromRoot]");
                }else{
                    if(temp.length==2)
                        conn.rm(temp[1]);
                    else
                        conn.rm(temp[1], temp[2]);
                }
            }

            if(splitMsg[0].equals("ls")){
                String[] temp = msg.split(" ");
                if(temp.length != 1 && temp.length != 2){
                    System.out.println("Error: function ls needs 0 or 1 argument \nls [dirName]\nls [pathFromRoot]");
                }else{
                    if(temp.length==1)

                        for(String f: conn.ls()){
                            System.out.printf("%10s %-10s", "|", f);
                            System.out.println();
                        }
                    else
                        for(String f: conn.lsFiles(temp[1])){
                            System.out.printf("%10s %-10s", "|", f);
                            System.out.println();
                        }
                }
            }

            if(splitMsg[0].equals("movFile")){
                String[] temp = msg.split(" ");
                if(temp.length != 3){
                    System.out.println("Error: function movFile needs 2 arguments \nmovFile [file] [destination]");
                }else{
                    conn.movFile(temp[1], temp[2]);
                }
            }

            if(splitMsg[0].equals("dwnFile")){
                String[] temp = msg.split(" ");
                if(temp.length != 3){
                    System.out.println("Error: function dwnFile needs 2 arguments \ndwnFile [file] [destination]");
                }else{
                    conn.dwnFile(temp[1], temp[2]);
                }
            }
            if(splitMsg[0].equals("limitSize")){
                String[] temp = msg.split(" ");
                if(temp.length != 2 && temp.length != 3){
                    System.out.println("Error: function limitSize needs 1 argument \nlimitSize [size]\nlimitSize [path] [size]");
                }else{
                    if(temp.length ==2){
                        conn.limitSize(Integer.parseInt(temp[1]));
                    }else {
                        conn.limitSize(temp[1], Integer.parseInt(temp[2]));
                    }
                }
            }
            if(splitMsg[0].equals("limitNum")){
                String[] temp = msg.split(" ");
                if(temp.length != 2 && temp.length != 3){
                    System.out.println("Error: function limitNum needs 1 argument \nlimitNum [num]\nlimitNum [path] [num]");
                }else{
                    if(temp.length ==2){
                        conn.limitFileNum(Integer.parseInt(temp[1]));
                    }else {
                        conn.limitFileNum(temp[1], Integer.parseInt(temp[2]));
                    }
                }
            }
            if(splitMsg[0].equals("blockExt")){
                String[] temp = msg.split(" ");
                if(temp.length != 2){
                    System.out.println("Error: function blockExt needs 1 argument \nblockExt [extension]");
                }else{
                    conn.blockExt(s.getPath()+"\\"+s.getName(), temp[1]);
                }
            }
            if(splitMsg[0].equals("allowExt")){
                String[] temp = msg.split(" ");
                if(temp.length != 2){
                    System.out.println("Error: function blockExt needs 1 argument \nblockExt [extension]");
                }else{
                    conn.unblockExt(temp[1]);
                }
            }
            if(splitMsg[0].equals("logout")){
                System.out.println("Logging out");
                conn.closeForUser();
                Storage temp = null;
                while(temp==null){
                    System.out.print(s.getName() + ": ");
                    msg = in.nextLine();
                    String[] split = msg.split(" ");

                    if(split.length != 3 || !split[0].equals("login")){
                        System.out.println("""
                                Error: to call any function you must be connected to storage.
                                \tUse: login [username] [password] to connect to current storage.
                                \tUse: login [pathToStorage] [username] [password] to connect to another storage.""");
                    }else{
                        String username = split[1];
                        String password = split[2];
                        try {
                            temp = conn.connect(path + "\\" + sName, username, password);
                        }catch(AuthException e){
                            e.printStackTrace();
                            continue;
                        }
                        System.out.print("Logged in  \n");
                    }
                }
            }
            if(splitMsg[0].equals("impFile")){
                String[] temp = msg.split(" ");
                if(temp.length != 3){
                    System.out.println("Error: function impFile needs 2 arguments \nimpFile [src] [dest]");
                }else{
                    conn.importFile(temp[1], temp[2]);
                }
            }

            if(splitMsg[0].equals("cd")){
                currDir = conn.cd(splitMsg[1]);
            }

            if(splitMsg[0].equals("stop")){
                conn.close();
                System.out.println("\nClosing the connection...");
                System.out.println("\nStopping the program...");
            }
        }


    }

}
