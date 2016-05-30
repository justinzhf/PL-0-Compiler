/**
 * Created by zhf on 2015/12/22.
 * 尚未判断超出Max值产生的错误
 * SymTable符号表，每一列的具体含义见TableRow
 */
public class SymbolTable {

    private int rowMax=10000;           //最大表长
    private int valueMax=100000;        //最大常量或变量值
    private int levelMax=3;                 //最深嵌套层次
    private int addressMax=10000;       //最大地址数



    private int myconst=1;                  //常量类型用1表示
    private int var=2;                      //变量类型用2表示
    private int proc=3;             //过程类型用3表示

    //TableRow是符号表中的每一行
    //tablePtr指向符号表中已经填入值最后一项的下一项
    //length表示符号表中填入；了多少行数据，实际上可以用tablePtr来表示
    private TableRow[] table=new TableRow[rowMax];          //rowMax行


    private int tablePtr=0;
    private int length=0;


    public void setTablePtr(int tablePtr) {
        this.tablePtr = tablePtr;
    }
    //初始化，全部为0
    public SymbolTable(){
        for(int i=0;i<rowMax;i++){
            table[i]=new TableRow();
            table[i].setAddress(0);
            table[i].setLevel(0);
            table[i].setSize(0);
            table[i].setType(0);
            table[i].setValue(0);
            table[i].setName(null);
        }
    }

    public int getVar() {
        return var;
    }

    public int getMyconst() {
        return myconst;
    }

    public int getProc() {
        return proc;
    }

    public int getLength(){
        return length;
    }

    //获取符号表中第i行
    public TableRow getRow(int i){
        return table[i];
    }
    /*
      *登录常量进符号表
      * 参数：
      * name：常量名
      * level：所在层次
      * value：值
      * address：相对于所在层次基地址的地址
    */
    public void enterConst(String name,int level,int value,int address){
        table[tablePtr].setName(name);
        table[tablePtr].setLevel(level);
        table[tablePtr].setValue(value);
        table[tablePtr].setAddress(address);
        table[tablePtr].setType(myconst);
        table[tablePtr].setSize(4);
        tablePtr++;
        length++;
    }



    /*
     *    登录变量进符号表
     *  参数同上
     *  说明：由于登录符号表操作都是在变量声明或常量声明或过程声明中调用，而PL/0不支持变量声明时赋值，所以不传入参数value
     *
    */
    public void enterVar(String name,int level,int address){
        table[tablePtr].setName(name);
        table[tablePtr].setLevel(level);
        table[tablePtr].setAddress(address);
        table[tablePtr].setType(var);
        table[tablePtr].setSize(0);
        tablePtr++;
        length++;
    }
    //登录过程进符号表，参数同上
    public void enterProc(String name,int level,int address){
        table[tablePtr].setName(name);
        table[tablePtr].setLevel(level);
        table[tablePtr].setAddress(address);
        table[tablePtr].setType(proc);
        table[tablePtr].setSize(0);
        tablePtr++;
        length++;
    }

    //在lev层之前，包括lev层，名字为name的变量、常量、或过程是否被定义，
    //使用变量、常量、或过程时调用该函数
    public boolean isPreExistSTable(String name,int lev){
        for(int i=0;i<length;i++){
            if(table[i].getName().equals(name)&&table[i].getLevel()<=lev){
                return true;
            }
        }
        return false;
    }

    //在lev层，名字为name的变量、常量、或过程是否被定义，
    //定义变量、常量、或过程时调用该函数
    public boolean isNowExistSTable(String name,int lev){
        for(int i=0;i<length;i++){
            if(table[i].getName().equals(name)&&table[i].getLevel()==lev){
                return true;
            }
        }
        return false;
    }


    //返回符号表中名字为name的行的行号
    public int  getNameRow(String name){
        for(int i=length-1;i>=0;i--){
            if(table[i].getName().equals(name)){
                return i;
            }
        }
        return -1;          //返回-1表示不存在该名字
    }
    public int getTablePtr() {
        return tablePtr;
    }

    public TableRow[] getAllTable(){
        return table;
    }

    //查找本层的过程在符号表中的位置
    public int getLevelPorc(int level){
        for(int i=length-1;i>=0;i--){
            if(table[i].getType()==proc){
                return i;
            }
        }
        return -1;
    }

}
