/**
 * Created by zhf on 2015/12/22.
 * TableRow是符号表中的每一行
 */
public class TableRow {

    private int type;           //表示常量、变量或过程
    private int value;          //表示常量或变量的值
    private int level;          //嵌套层次，最大应为3，不在这里检查其是否小于等于，在SymbolTable中检查
    private int address;      //相对于所在嵌套过程基地址的地址
    private int size;         //表示常量，变量，过程所占的大小，此变量和具体硬件有关，实际上在本编译器中为了方便，统一设为4了,设置过程在SymTable中的三个enter函数中
    private String name;        //变量、常量或过程名

    public int  getType(){
        return type;
    }

    public int getValue(){
        return value;
    }

    public int getLevel(){
        return level;
    }

    public int getAddress(){
        return address;
    }

    public int getSize(){
        return size;
    }

    public String getName(){
        return name;
    }

    public void setType(int t){
        type=t;
    }

    public void setValue(int v){
        value=v;
    }

    public void setLevel(int L){
        level=L;
    }

    public void setAddress(int a){
        address=a;
    }

    public void setSize(int s){
        size=s;
    }

    public void setName(String s){
        name=s;
    }

}
