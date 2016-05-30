/**
 * Created by zhf on 2015/10/28.
 */

public class RValue {
    private int id;
    private int line;
    private String value;


    public void setId(int _id){
        id=_id;
    }
    public void setValue(String _value){
        value=_value;
    }
    public void setLine(int _line){
        line=_line;
    }

    public int getId(){
        return id;
    }
    public int getLine(){
        return line;
    }
    public String getValue(){
        return value;
    }

}
