package Util.Emoji;

public class Emoji {

    private int id;
    private String name;
    private String character;

    public Emoji(int id, String name, String character) {
        this.id = id;
        this.name = name;
        this.character = character;
    }

    // Getter
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCharacter() {
        return character;
    }

    // Setter
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCharacter(String character) {
        this.character = character;
    }


    @Override
    public String toString() {
        return "Emoji{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", character='" + character + '\'' +
                '}';
    }
}