package Util.Emoji;

public class Emoji {

    private final int id;
    private final String name;
    private final String character;

    public Emoji(int id, String name, String character) {
        this.id = id;
        this.name = name;
        this.character = character;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCharacter() {
        return character;
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