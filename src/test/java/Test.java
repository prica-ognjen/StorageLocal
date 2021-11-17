import java.io.File;

public class Test {

    public static void main(String[] args) {

        String test = "C:/Users/Ognjen/Desktop";
        String test2 = "C\\:users\\ognjen\\desktop\\storagetests";

        String name = "dir";

        File f  = new File(test+"/"+name);
        boolean created = f.mkdir();

        System.out.println(f.getPath());
        System.out.println(f.getName());
        System.out.println(f.getParent());

        System.out.println(created);

    }

}
