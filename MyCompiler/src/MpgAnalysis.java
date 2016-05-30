import com.sun.org.glassfish.external.arc.Stability;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by zhf on 2015/10/29.
 * 关键字const对应的变量是con
 * 而常数对应的变量是const
 * 语法分析没有错误的版本，版本1.3和版本1.4不再关注
 * 常量登入符号表：将地址加1
 * 变量登录符号表：将地址加1
 * 过程登录符号表：首先将level加1，传入参数将address置0，过程完成后再将level减一，再将address恢复，
 * 亟待完成的工作：本版本没有实现将address恢复的操作，应首先实现该功能
 *
 * 2015/12/26
 * 符号表，目标代码的生成检查无错误
 * 测试程序：test2，test3；对应的是bh2，bhtest
 * 2015/12/27
 * 完成了解释运行的工作，只剩下错误处理了
 * 错误处理完成，只剩下过程传递参数的问题了
 * 解决了不同层次的变量定义重名的问题
 * 完成了参数传递
 */
import java.io.*;
public class MpgAnalysis {
    private static  int PROG=1;//program
    private static  int BEG=2;//begin
    private static  int END=3;//end
    private static  int IF=4;//if
    private static  int THEN=5;//then
    private static  int ELS=6;//else
    private static  int CON=7;//const
    private static  int PROC=8;//procdure
    private static  int VAR=9;//var
    private static  int DO=10;//do
    private static  int WHI=11;//while
    private static  int CAL=12;//call
    private static  int REA=13;//read
    private static  int WRI=14;//write
    private static  int REP=15;//repeate
    private static  int ODD=16;//  oddl      和keyWord中每个字的序号是相等的

    private static  int EQU=17;//"="
    private static  int LES=18;//"<"
    private static  int LESE=19;//"<="
    private static  int LARE=20;//">="
    private static  int LAR=21;//">"
    private static  int NEQE=22;//"<>"


    private static  int ADD=23;//"+"
    private static  int SUB=24;//"-"
    private static  int MUL=25;//"*"
    private static  int DIV=26;//"/"

    private static  int SYM=27;//标识符
    private static  int CONST=28;//常量

    private static  int CEQU=29;//":="

    private static  int COMMA=30;//","
    private static  int SEMIC=31;//";"
    private static  int POI=32;//"."
    private static  int LBR=33;//"("
    private static  int RBR=34;//")"

    LexAnalysis lex;
    private boolean errorHapphen=false;
    private int rvLength=1000;
    private RValue[] rv=new RValue[rvLength];
    private int terPtr=0;       //RValue的迭代器

    private SymbolTable STable=new SymbolTable();       //符号表
    private AllPcode  Pcode=new AllPcode();                 //存放目标代码


    private int level=0;                //主程序为第0层
    private int address=0;             //主程序或变量的声明是为0
    private int addrIncrement=1;//TabelRow中的address的增量，常量、过程的定义登录进符号表均不会使其增加，但是为什么

    public MpgAnalysis(String filename){
        for(int i=0;i<rvLength;i++){
            rv[i]=new RValue();
            rv[i].setId(-2);
            rv[i].setValue("-2");
        }
        lex=new LexAnalysis(filename);
    }

    public void readLex(){
        String filename="lex.txt";
        File file=new File(filename);
        BufferedReader ints=null;
        String tempLex,temp[];
        try{
            ints=new BufferedReader(new FileReader(file));
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        try{
            int i=0;
            while((tempLex=ints.readLine())!=null) {
                temp = tempLex.split(" ");
                rv[i].setId(Integer.parseInt(temp[0], 10));
                rv[i].setValue(temp[1]);
                rv[i].setLine(Integer.parseInt(temp[2]));
                i++;
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void prog(){
        if(rv[terPtr].getId()==PROG){
            terPtr++;
            if(rv[terPtr].getId()!=SYM){
                errorHapphen=true;
                showError(1,"");
            }else{
                terPtr++;
                if(rv[terPtr].getId()!=SEMIC){
                    errorHapphen=true;
                    showError(0,"");
                    return;
                }else{
                    terPtr++;
                    block();
                }
            }
        }else {
            errorHapphen = true;
            showError(2,"");
            return;
        }
    }

    public void block(){
        int addr0=address;      //记录本层之前的数据量，以便恢复时返回
        int tx0=STable.getTablePtr();       //记录本层名字的初始位置
        int cx0;
        int  propos=0;
        if(tx0>0){
            propos=STable.getLevelPorc(level);
            tx0=tx0- STable.getRow(propos).getSize();   //记录本本层变量的开始位置
        }
        if(tx0==0){
            address=3;      //每一层最开始位置的三个空间用来存放静态连SL、动态连DL、和返回地址RA
        }else{
            //每一层最开始位置的三个空间用来存放静态连SL、动态连DL、和返回地址RA
            //紧接着放形参的个数
            address=3+STable.getAllTable()[propos].getSize();


        }


        //暂存当前Pcode.codePtr的值，即jmp,0,0在codePtr中的位置，用来一会回填
        int tempCodePtr= Pcode.getCodePtr();
        Pcode.gen(Pcode.getJMP(),0,0);


        if(rv[terPtr].getId()==CON){//此处没有terPtr++
            condecl();
        } if(rv[terPtr].getId()==VAR){
            vardecl();
        } if(rv[terPtr].getId()==PROC){
            proc();
            level--;
        }
        /*
        * 声明部分完成，进入语句处理部分，之前生成的jmp，0，0应当跳转到这个位置
        *
        * */
        //回填jmp，0，0的跳转地址
        if(tx0>0){
            for(int i=0;i<STable.getAllTable()[propos].getSize();i++){
                Pcode.gen(Pcode.getSTO(),0,STable.getAllTable()[propos].getSize()+3-1-i);
            }
        }
        Pcode.getPcodeArray()[tempCodePtr].setA(Pcode.getCodePtr());
        Pcode.gen(Pcode.getINT(),0,address);        //生成分配内存的代码
        if(tx0==0){
           // STable.getRow(tx0).setValue(Pcode.getCodePtr());     //将本过程在符号表中的值设为本过程执行语句开始的位置
        }else {
            STable.getRow(propos).setValue(Pcode.getCodePtr()-1-STable.getAllTable()[propos].getSize());     //将本过程在符号表中的值设为本过程执行语句开始的位置

        }

        body();
        Pcode.gen(Pcode.getOPR(),0,0);      //生成退出过程的代码，若是主程序，则直接退出程序

        address=addr0;      //分程序结束，恢复相关值
        STable.setTablePtr(tx0);

    }

    public void condecl(){          //const的level是无意义的吗
        if(rv[terPtr].getId()==CON){
            terPtr++;
            myconst();
            while(rv[terPtr].getId()==COMMA){
                terPtr++;
                myconst();
            }
            if(rv[terPtr].getId()!=SEMIC){
                errorHapphen=true;
                showError(0,"");
                return;
            }else{
                terPtr++;
            }
        }else{
            errorHapphen=true;
            showError(-1,"");
            return;
        }
    }

    public void myconst(){
        String name;
        int value;
        if(rv[terPtr].getId()==SYM){
            name=rv[terPtr].getValue();
            terPtr++;
            if(rv[terPtr].getId()==CEQU){
                terPtr++;
                if(rv[terPtr].getId()==CONST){
                    value=Integer.parseInt(rv[terPtr].getValue());
                    if(STable.isNowExistSTable(name, level)){
                        errorHapphen=true;
                        showError(15,name);
                    }
                    STable.enterConst(name,level,value,address);
//                   address+=addrIncrement;             //登录符号表后地址加1指向下一个
                    terPtr++;
                }
            }else{
                errorHapphen=true;
                showError(3,"");
                return;
            }
        }else {
            errorHapphen=true;
            showError(1,"");
            return;
        }
    }

    public void vardecl(){
        String name;
        int value;
        if(rv[terPtr].getId()==VAR){
            terPtr++;
            if(rv[terPtr].getId()==SYM){
                name=rv[terPtr].getValue();
                if(STable.isNowExistSTable(name, level)){
                    errorHapphen=true;
                    showError(15,name);
                }
                STable.enterVar(name,level,address);
                address+=addrIncrement;
                terPtr++;
                while(rv[terPtr].getId()==COMMA){
                    terPtr++;
                    if(rv[terPtr].getId()==SYM){
                        name=rv[terPtr].getValue();
                        if(STable.isNowExistSTable(name, level)){
                            errorHapphen=true;
                            showError(15,name);
                        }
                        STable.enterVar(name,level,address);
                        address+=addrIncrement;     //地址加1登录符号表
                        terPtr++;
                    }else{
                        errorHapphen=true;
                        showError(1,"");
                        return;
                    }
                }
                if(rv[terPtr].getId()!=SEMIC){
                    errorHapphen=true;
                    showError(0,"");
                    return;
                }else{
                    terPtr++;
                }
            }else {
                errorHapphen=true;
                showError(1,"");
                return;
            }

        }else{
            errorHapphen=true;
            showError(-1,"");
            return;
        }
    }

    public void proc(){
        if(rv[terPtr].getId()==PROC){
            terPtr++;
            //id();
            int count=0;//用来记录proc中形参的个数
            int propos;// 记录本proc在符号表中的位置
            if(rv[terPtr].getId()==SYM){
                String name=rv[terPtr].getValue();
                if(STable.isNowExistSTable(name, level)){
                    errorHapphen=true;
                    showError(15,name);
                }
                propos=STable.getTablePtr();
                STable.enterProc(rv[terPtr].getValue(),level,address);
                level++;                //level值加一，因为其后的所有定义均在该新的proc中完成
                terPtr++;
                if(rv[terPtr].getId()==LBR){
                    terPtr++;
                    //id();
                    if(rv[terPtr].getId()==SYM){
                        STable.enterVar(rv[terPtr].getValue(),level,3+count) ;      //3+count+1为形参在存储空间中的位置
                        count++;
                        STable.getAllTable()[propos].setSize(count);        //用本过程在符号表中的size域记录形参的个数
                        terPtr++;
                        while(rv[terPtr].getId()==COMMA){
                            terPtr++;
                            if(rv[terPtr].getId()==SYM){
                                STable.enterVar(rv[terPtr].getValue(),level,3+count) ;      //3+count+1为形参在存储空间中的位置
                                count++;
                                STable.getAllTable()[propos].setSize(count);        //用本过程在符号表中的size域记录形参的个数
                                terPtr++;
                            }else{
                                errorHapphen=true;
                                showError(1,"");
                                return;
                            }
                        }
                    }
                    if(rv[terPtr].getId()==RBR){
                        terPtr++;
                        if(rv[terPtr].getId()!=SEMIC){
                            errorHapphen=true;
                            showError(0,"");
                            return;
                        }else{
                            terPtr++;
                            block();
                            while(rv[terPtr].getId()==SEMIC){
                                terPtr++;
                                proc();
                            }
                        }
                    }else{
                        errorHapphen=true;
                        showError(5,"");
                        return;
                    }

                }else{
                    errorHapphen=true;
                    showError(4,"");
                    return;
                }
            }else{
                errorHapphen=true;
                showError(1,"");
                return;
            }

        }else{
            errorHapphen=true;
            showError(-1,"");
            return;
        }
    }

    public void body(){
        if(rv[terPtr].getId()==BEG){
            terPtr++;
            statement();
            while(rv[terPtr].getId()==SEMIC){
                terPtr++;
                statement();
            }
            if(rv[terPtr].getId()==END){
                terPtr++;
            }else{
                errorHapphen=true;
                showError(7,"");
                return;
            }
        }else{
            errorHapphen=true;
            showError(6,"");
            return;
        }
    }

    public void statement(){
        if(rv[terPtr].getId()==IF){
            int cx1;
            terPtr++;
            lexp();
            if(rv[terPtr].getId()==THEN){
                cx1=Pcode.getCodePtr();             //用cx1记录jpc ，0，0（就是下面这一条语句产生的目标代码）在Pcode中的地址，用来一会回填
                Pcode.gen(Pcode.getJPC(),0,0);  //产生条件转移指令，条件的bool值为0时跳转，跳转的目的地址暂时填为0
                terPtr++;
                statement();
                int cx2=Pcode.getCodePtr();
                Pcode.gen(Pcode.getJMP(),0,0);
                Pcode.getPcodeArray()[cx1].setA(Pcode.getCodePtr());        //地址回填，将jpc，0，0中的A回填
                Pcode.getPcodeArray()[cx2].setA(Pcode.getCodePtr());
                if(rv[terPtr].getId()==ELS){
                    terPtr++;
                    statement();
                    Pcode.getPcodeArray()[cx2].setA(Pcode.getCodePtr());
                }//没了？
            }else{
                errorHapphen=true;
                showError(8,"");
                return;
            }
        }else if(rv[terPtr].getId()==WHI){
            int cx1=Pcode.getCodePtr();     //保存条件表达式在Pcode中的地址
            terPtr++;
            lexp();
            if(rv[terPtr].getId()==DO){
                int cx2=Pcode.getCodePtr();     //保存条件跳转指令的地址，在回填时使用，仍是条件不符合是跳转
                Pcode.gen(Pcode.getJPC(),0,0);
                terPtr++;
                statement();
                Pcode.gen(Pcode.getJMP(),0,cx1);    //完成DO后的相关语句后，需要跳转至条件表达式处，检查是否符合条件，即是否继续循环
                Pcode.getPcodeArray()[cx2].setA(Pcode.getCodePtr());        //回填条件转移指令
            }else{
                errorHapphen=true;
                showError(9,"");
                return;
            }
        }else if(rv[terPtr].getId()==CAL){
            terPtr++;
            //id();
            int count=0;//用来检验传入的参数和设定的参数是否相等
            TableRow tempRow;
            if(rv[terPtr].getId()==SYM){
                if(STable.isPreExistSTable(rv[terPtr].getValue(),level)){        //符号表中存在该标识符
                     tempRow=STable.getRow(STable.getNameRow(rv[terPtr].getValue()));  //获取该标识符所在行的所有信息，保存在tempRow中
                    if(tempRow.getType()==STable.getProc()) { //判断该标识符类型是否为procedure，SymTable中procdure类型用proc变量来表示，
                        ;
                    }       //if类型为proc
                    else{       //cal类型不一致的错误
                        errorHapphen=true;
                        showError(11,"");
                        return;
                    }
                }       //if符号表中存在标识符
                else{           //cal 未定义变量的错误
                    errorHapphen=true;
                    showError(10,"");
                    return;
                }
                terPtr++;
                if(rv[terPtr].getId()==LBR){
                    terPtr++;
                    if(rv[terPtr].getId()==RBR){
                        terPtr++;
                        Pcode.gen(Pcode.getCAL(),level-tempRow.getLevel(),tempRow.getValue());        //调用过程中的保存现场由解释程序完成，这里只产生目标代码,+3需详细说明
                    }else{
                        exp();
                        count++;
                        while(rv[terPtr].getId()==COMMA){
                            terPtr++;
                            exp();
                            count++;
                        }
                        if(count!=tempRow.getSize()){
                            errorHapphen=true;
                            showError(16,tempRow.getName());
                            return;
                        }
                        Pcode.gen(Pcode.getCAL(),level-tempRow.getLevel(),tempRow.getValue());        //调用过程中的保存现场由解释程序完成，这里只产生目标代码,+3需详细说明
                        if(rv[terPtr].getId()==RBR){
                            terPtr++;
                        }else{
                            errorHapphen=true;
                            showError(5,"");
                            return;
                        }
                    }
                }else{
                    errorHapphen=true;
                    showError(4,"");
                    return;
                }
            }else{
                errorHapphen=true;
                showError(1,"");
                return;
            }

        }else if(rv[terPtr].getId()==REA){
            terPtr++;
            if(rv[terPtr].getId()==LBR){
                terPtr++;
                //      id();
                if(rv[terPtr].getId()==SYM){
                    if(!STable.isPreExistSTable((rv[terPtr].getValue()),level)){      //首先判断在符号表中在本层或本层之前是否有此变量
                        errorHapphen=true;
                        showError(10,"");
                        return;

                    }//if判断在符号表中是否有此变量
                    else{           //sto未定义变量的错误
                        TableRow tempTable=STable.getRow(STable.getNameRow(rv[terPtr].getValue()));
                        if(tempTable.getType()==STable.getVar()){       //该标识符是否为变量类型
                            Pcode.gen(Pcode.getOPR(),0,16);         //OPR 0 16	从命令行读入一个输入置于栈顶   //层差的含义所在？？直接用嵌套的层次数作为参数不可以吗？
                            Pcode.gen(Pcode.getSTO(),level-tempTable.getLevel(),tempTable.getAddress());  //STO L ，a 将数据栈栈顶的内容存入变量（相对地址为a，层次差为L）
                        }//if标识符是否为变量类型
                        else{       //sto类型不一致的错误
                            errorHapphen=true;
                            showError(12,"");
                            return;
                        }
                    }
                    terPtr++;
                    while(rv[terPtr].getId()==COMMA){
                        terPtr++;
                        if(rv[terPtr].getId()==SYM){
                            if(!STable.isPreExistSTable((rv[terPtr].getValue()),level)){      //首先判断在符号表中是否有此变量
                                errorHapphen=true;
                                showError(10,"");
                                return;

                            }//if判断在符号表中是否有此变量
                            else{           //sto未定义变量的错误
                                TableRow tempTable=STable.getRow(STable.getNameRow(rv[terPtr].getValue()));
                                if(tempTable.getType()==STable.getVar()){       //该标识符是否为变量类型
                                    Pcode.gen(Pcode.getOPR(),0,16);         //OPR 0 16	从命令行读入一个输入置于栈顶   //层差的含义所在？？直接用嵌套的层次数作为参数不可以吗？
                                    Pcode.gen(Pcode.getSTO(),level-tempTable.getLevel(),tempTable.getAddress());  //STO L ，a 将数据栈栈顶的内容存入变量（相对地址为a，层次差为L）
                                }//if标识符是否为变量类型
                                else{       //sto类型不一致的错误
                                    errorHapphen=true;
                                    showError(12,"");
                                    return;
                                }
                            }
                            terPtr++;
                        }else{
                            errorHapphen=true;
                            showError(1,"");
                            return;
                        }
                    }
                    if(rv[terPtr].getId()==RBR){
                        terPtr++;
                    }else{
                        errorHapphen=true;
                        showError(25,"");
                    }
                }else{
                    errorHapphen=true;
                    showError(26,"");
                }
            }else{
                errorHapphen=true;
                showError(4,"");
                return;
            }
        }else if(rv[terPtr].getId()==WRI){
            terPtr++;
            if(rv[terPtr].getId()==LBR){
                terPtr++;
                exp();
                Pcode.gen(Pcode.getOPR(),0,14);         //输出栈顶的值到屏幕
                while(rv[terPtr].getId()==COMMA){
                    terPtr++;
                    exp();
                    Pcode.gen(Pcode.getOPR(),0,14);         //输出栈顶的值到屏幕
                }

                Pcode.gen(Pcode.getOPR(),0,15);         //输出换行
                if(rv[terPtr].getId()==RBR){
                    terPtr++;
                }else{
                    errorHapphen=true;
                    showError(5,"");
                    return;
                }
            }else{
                errorHapphen=true;
                showError(4,"");
                return;
            }
        }else if(rv[terPtr].getId()==BEG){//这里也没有terPtr++;          //body不生成目标代码
            body();
        }else if(rv[terPtr].getId()==SYM){      //赋值语句
            String name=rv[terPtr].getValue();
            terPtr++;
            if(rv[terPtr].getId()==CEQU){
                terPtr++;
                exp();
                if(!STable.isPreExistSTable(name,level)){        //检查标识符是否在符号表中存在
                    errorHapphen=true;
                    showError(14,name);
                    return;
                }//if判断在符号表中是否有此变量
                else{           //sto未定义变量的错误
                    TableRow tempTable=STable.getRow(STable.getNameRow(name));
                    if(tempTable.getType()==STable.getVar()){           //检查标识符是否为变量类型
                        Pcode.gen(Pcode.getSTO(),level-tempTable.getLevel(),tempTable.getAddress());  //STO L ，a 将数据栈栈顶的内容存入变量
                    }////检查标识符是否为变量类型
                    else{       //类型不一致的错误
                        errorHapphen=true;
                        showError(13,name);
                        return;
                    }
                }
            }else{
                errorHapphen=true;
                showError(3,"");
                return;
            }
        }else{
            errorHapphen=true;
            showError(1,"");
            return;
        }
    }

    public void lexp(){
        if(rv[terPtr].getId()==ODD){
            terPtr++;
            exp();
            Pcode.gen(Pcode.getOPR(),0,6);  //OPR 0 6	栈顶元素的奇偶判断，结果值在栈顶
        }else{
            exp();
            int loperator=lop();        //返回值用来产生目标代码，如下
            exp();
            if(loperator==EQU){
                Pcode.gen(Pcode.getOPR(),0,8);      //OPR 0 8	次栈顶与栈顶是否相等，退两个栈元素，结果值进栈
            }else if(loperator==NEQE){
                Pcode.gen(Pcode.getOPR(),0,9);      //OPR 0 9	次栈顶与栈顶是否不等，退两个栈元素，结果值进栈
            }else if(loperator==LES){
                Pcode.gen(Pcode.getOPR(),0,10);     //OPR 0 10	次栈顶是否小于栈顶，退两个栈元素，结果值进栈
            }else if(loperator==LESE){
                Pcode.gen(Pcode.getOPR(),0,13);     // OPR 0 13	次栈顶是否小于等于栈顶，退两个栈元素，结果值进栈
            }else if(loperator==LAR){
                Pcode.gen(Pcode.getOPR(),0,12);     //OPR 0 12	次栈顶是否大于栈顶，退两个栈元素，结果值进栈
            }else if(loperator==LARE){
                Pcode.gen(Pcode.getOPR(),0,11);     //OPR 0 11	次栈顶是否大于等于栈顶，退两个栈元素，结果值进栈
            }
        }
    }

    public void exp(){
        int tempId=rv[terPtr].getId();
        if(rv[terPtr].getId()==ADD){
            terPtr++;
        }else if(rv[terPtr].getId()==SUB){
            terPtr++;
        }
        term();
        if(tempId==SUB){
            Pcode.gen(Pcode.getOPR(),0,1);      //  OPR 0 1	栈顶元素取反
        }
        while(rv[terPtr].getId()==ADD||rv[terPtr].getId()==SUB){
            tempId=rv[terPtr].getId();
            terPtr++;
            term();
            if(tempId==ADD){
                Pcode.gen(Pcode.getOPR(),0,2);       //OPR 0 2	次栈顶与栈顶相加，退两个栈元素，结果值进栈
            }else if(tempId==SUB){
                Pcode.gen(Pcode.getOPR(),0,3);      //OPR 0 3	次栈顶减去栈顶，退两个栈元素，结果值进栈
            }
        }
    }

    public void term(){
        factor();
        while(rv[terPtr].getId()==MUL||rv[terPtr].getId()==DIV){
            int tempId=rv[terPtr].getId();
            terPtr++;
            factor();
            if(tempId==MUL){
                Pcode.gen(Pcode.getOPR(),0,4);       //OPR 0 4	次栈顶乘以栈顶，退两个栈元素，结果值进栈
            }else if(tempId==DIV){
                Pcode.gen(Pcode.getOPR(),0,5);      // OPR 0 5	次栈顶除以栈顶，退两个栈元素，结果值进栈
            }
        }
    }

    public void factor(){
        if(rv[terPtr].getId()==CONST){
            Pcode.gen(Pcode.getLIT(),0,Integer.parseInt(rv[terPtr].getValue()));    //是个数字,  LIT 0 a 取常量a放入数据栈栈顶
            terPtr++;
        }else if(rv[terPtr].getId()==LBR){
            terPtr++;
            exp();
            if(rv[terPtr].getId()==RBR){
                terPtr++;
            }else{
                errorHapphen=true;
                showError(5,"");
            }
        }else if(rv[terPtr].getId()==SYM){
            String name=rv[terPtr].getValue();
            if(!STable.isPreExistSTable(name,level)){     //判断标识符在符号表中是否存在
                errorHapphen=true;
                showError(10,"");
                return;
            }//if判断在符号表中是否有此变量
            else{           //未定义变量的错误
                TableRow tempRow= STable.getRow(STable.getNameRow(name));
                if(tempRow.getType()==STable.getVar()){ //标识符是变量类型
                    Pcode.gen(Pcode.getLOD(),level-tempRow.getLevel(),tempRow.getAddress());    //变量，LOD L  取变量（相对地址为a，层差为L）放到数据栈的栈顶
                }else if (tempRow.getType()==STable.getMyconst()){
                    Pcode.gen(Pcode.getLIT(),0,tempRow.getValue());         //常量，LIT 0 a 取常量a放入数据栈栈顶
                }
                else{       //类型不一致的错误
                    errorHapphen=true;
                    showError(12,"");
                    return;
                }
            }
            terPtr++;
        }else {
            errorHapphen=true;
            showError(1,"");
        }
    }

    public int lop(){
        String loperator;
        if(rv[terPtr].getId()==EQU){
            terPtr++;
            return EQU;
        }else if(rv[terPtr].getId()==NEQE){
            terPtr++;
            return NEQE;
        }else if(rv[terPtr].getId()==LES){
            terPtr++;
            return LES;
        }else if(rv[terPtr].getId()==LESE){
            terPtr++;
            return LESE;
        }else if(rv[terPtr].getId()==LAR){
            terPtr++;
            return LAR;
        }else if(rv[terPtr].getId()==LARE){
            terPtr++;
            return LARE;
        }
        return -1;
    }

    // public void id(){

    //    }
    public boolean mgpAnalysis(){
        lex.bAnalysis();
        readLex();
        prog();
        return errorHapphen;
    }

    public void showtable(){
        System.out.println("type,name,level,address,value,size");
        for(int i=0;i<STable.getLength();i++){
            System.out.println(STable.getRow(i).getType()+"  "+ STable.getRow(i).getName()+"  "+STable.getRow(i).getLevel()+"  "+STable.getRow(i).getAddress()+"  "+STable.getRow(i).getValue()+
            "  "+STable.getRow(i).getSize());
        }
    }

    public void showPcode(){
        for(int i=0;i<Pcode.getCodePtr();i++){
           switch (Pcode.getPcodeArray()[i].getF()){
               case 0:
                   System.out.print("LIT  ");
                   break;
               case 1:
                   System.out.print("OPR  ");
                   break;
               case 2:
                   System.out.print("LOD  ");
                   break;
               case 3:
                   System.out.print("STO  ");
                   break;
               case 4:
                   System.out.print("CAL  ");
                   break;
               case 5:
                   System.out.print("INT  ");
                   break;
               case 6:
                   System.out.print("JMP  ");
                   break;
               case 7:
                   System.out.print("JPC  ");
                   break;
               case 8:
                   System.out.print("RED  ");
                   break;
               case 9:
                   System.out.print("WRI  ");
                   break;
           }
            System.out.println(Pcode.getPcodeArray()[i].getL()+"  "+Pcode.getPcodeArray()[i].getA());
        }
    }

    public void showPcodeInStack(){
        Interpreter inter=new Interpreter();
        inter.setPcode(Pcode);
        for(int i=0;i<inter.getCode().getCodePtr();i++){
            switch (inter.getCode().getPcodeArray()[i].getF()){
                case 0:
                    System.out.print("LIT  ");
                    break;
                case 1:
                    System.out.print("OPR  ");
                    break;
                case 2:
                    System.out.print("LOD  ");
                    break;
                case 3:
                    System.out.print("STO  ");
                    break;
                case 4:
                    System.out.print("CAL  ");
                    break;
                case 5:
                    System.out.print("INT  ");
                    break;
                case 6:
                    System.out.print("JMP  ");
                    break;
                case 7:
                    System.out.print("JPC  ");
                    break;
                case 8:
                    System.out.print("RED  ");
                    break;
                case 9:
                    System.out.print("WRI  ");
                    break;

            }
            System.out.println(inter.getCode().getPcodeArray()[i].getL()+"  "+inter.getCode().getPcodeArray()[i].getA());
        }


    }

    public void showError(int i,String name){
        switch (i){
            case -1:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("wrong token");        //常量定义不是const开头,变量定义不是var 开头
                break;
            case 0:
                System.out.print("ERROR "+i+" "+"in line " + (rv[terPtr].getLine()-1)+":");
                System.out.println("Missing semicolon");        //缺少分号
                break;
            case 1:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Identifier illegal");       //标识符不合法
                break;
            case 2:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("The beginning of program must be 'program'");       //程序开始第一个字符必须是program
                break;
            case 3:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Assign must be ':='");       //赋值没用：=
                break;
            case 4:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing '('");       //缺少左括号
                break;
            case 5:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing ')'");       //缺少右括号
                break;
            case 6:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing 'begin'");       //缺少begin
                break;
            case 7:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing 'end'");       //缺少end
                break;
            case 8:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing 'then'");       //缺少then
                break;
            case 9:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing 'do'");       //缺少do
                break;
            case 10:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Not exist "+"'"+rv[terPtr].getValue()+"'");       //call，write，read语句中，不存在标识符
                break;
            case 11:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("'"+rv[terPtr].getValue()+"'"+"is not a procedure");       //该标识符不是proc类型
                break;
            case 12:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("'"+rv[terPtr].getValue()+"'"+"is not a variable");       //read，write语句中，该标识符不是var类型
                break;
            case 13:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("'"+name+"'"+"is not a variable");       //赋值语句中，该标识符不是var类型
                break;
            case 14:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Not exist"+"'"+name+"'");       //赋值语句中，该标识符不存在
                break;
            case 15:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Already exist"+"'"+name+"'");       //该标识符已经存在
                break;
            case 16:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Number of parameters of procedure "+"'"+name+"'"+"is incorrect");       //该标识符已经存在
                break;
        }

    }
    public void interpreter(){
        if(errorHapphen){
            return;
        }
        Interpreter inter=new Interpreter();
        inter.setPcode(Pcode);
        inter.interpreter();
    }

}

