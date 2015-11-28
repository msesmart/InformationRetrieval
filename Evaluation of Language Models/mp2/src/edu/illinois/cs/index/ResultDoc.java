package edu.illinois.cs.index;

public class ResultDoc {
    private int _id;
    private String _title = "[no title]";
    private String _content = "[no content]";

    public ResultDoc(int id) {
        _id = id;
    }

    public int id() {
        return _id;
    }

    public String title() {
        return _title;
    }

    public ResultDoc title(String nTitle) {
        _title = nTitle;
        return this;
    }

    public String content() {
        return _content;
    }

    public ResultDoc content(String nContent) {
        _content = nContent;
        return this;
    }
}
