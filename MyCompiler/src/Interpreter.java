import java.util.Scanner;

/**
 * Created by zhf on 2015/12/26.
 */
public class Interpreter {
        /*
    *    代码的具体形式：
    *    FLA
    *    其中：F段代表伪操作码
    *    L段代表调用层与说明层的层差值
    *    A段代表位移量（相对地址）
    *    进一步说明：
    *    INT：为被调用的过程（包括主过程）在运行栈S中开辟数据区，这时A段为所需数据单元个数（包括三个连接数据）；L段恒为0。
    *    CAL：调用过程，这时A段为被调用过程的过程体（过程体之前一条指令）在目标程序区的入口地址。
    *    LIT：将常量送到运行栈S的栈顶，这时A段为常量值。
    *    LOD：将变量送到运行栈S的栈顶，这时A段为变量所在说明层中的相对位置。
    *    STO：将运行栈S的栈顶内容送入某个变量单元中，A段为变量所在说明层中的相对位置。
    *    JMP：无条件转移，这时A段为转向地址（目标程序）。
    *    JPC：条件转移，当运行栈S的栈顶的布尔值为假（0）时，则转向A段所指目标程序地址；否则顺序执行。
    *    OPR：关系或算术运算，A段指明具体运算，例如A=2代表算术运算“＋”；A＝12代表关系运算“>”等等。运算对象取自运行栈S的栈顶及次栈顶。
    *
    *    OPR 0 0	过程调用结束后,返回调用点并退栈
    *    OPR 0 1	栈顶元素取反
    *    OPR 0 2	次栈顶与栈顶相加，退两个栈元素，结果值进栈
    *    OPR 0 3	次栈顶减去栈顶，退两个栈元素，结果值进栈
    *    OPR 0 4	次栈顶乘以栈顶，退两个栈元素，结果值进栈
    *    OPR 0 5	次栈顶除以栈顶，退两个栈元素，结果值进栈
    *    OPR 0 6	栈顶元素的奇偶判断，结果值在栈顶
    *    OPR 0 7
    *    OPR 0 8	次栈顶与栈顶是否相等，退两个栈元素，结果值进栈
    *    OPR 0 9	次栈顶与栈顶是否不等，退两个栈元素，结果值进栈
    *    OPR 0 10	次栈顶是否小于栈顶，退两个栈元素，结果值进栈
    *    OPR 0 11	次栈顶是否大于等于栈顶，退两个栈元素，结果值进栈
    *    OPR 0 12	次栈顶是否大于栈顶，退两个栈元素，结果值进栈
    *    OPR 0 13	次栈顶是否小于等于栈顶，退两个栈元素，结果值进栈
    *    OPR 0 14	栈顶值输出至屏幕,并且输出一个空格
    *    OPR 0 15	屏幕输出换行
    *    OPR 0 16	从命令行读入一个输入置于栈顶
    *
    private int LIT = 0;           //LIT 0 ，a 取常量a放入数据栈栈顶
    private int OPR = 1;        //OPR 0 ，a 执行运算，a表示执行某种运算，具体是何种运算见上面的注释
    private int LOD = 2;        //LOD L ，a 取变量（相对地址为a，层差为L）放到数据栈的栈顶
    private int STO = 3;        //STO L ，a 将数据栈栈顶的内容存入变量（相对地址为a，层次差为L）
    private int CAL = 4;        //CAL L ，a 调用过程（转子指令）（入口地址为a，层次差为L）
    private int INT = 5;         //INT 0 ，a 数据栈栈顶指针增加a
    private int JMP = 6;       //JMP 0 ，a无条件转移到地址为a的指令
    private int JPC = 7;        //JPC 0 ，a 条件转移指令，转移到地址为a的指令
    private int RED = 8;       //RED L ，a 读数据并存入变量（相对地址为a，层次差为L）
    private int WRT = 9;      //WRT 0 ，0 将栈顶内容输出

?	假想机的结构
两个存储器：存储器CODE，用来存放P的代码
            数据存储器STACK（栈）用来动态分配数据空间
四个寄存器：
一个指令寄存器I:存放当前要执行的代码
一个栈顶指示器寄存器T：指向数据栈STACK的栈顶
一个基地址寄存器B：存放当前运行过程的数据区在STACK中的起始地址
一个程序地址寄存器P：存放下一条要执行的指令地址
该假想机没有供运算用的寄存器。所有运算都要在数据栈STACK的栈顶两个单元之间进行，并用运算结果取代原来的两个运算对象而保留在栈顶
?	活动记录：
 RA
 SL
 DL

RA：返回地址
SL：保存该过程直接外层的活动记录首地址
DL：调用者的活动记录首地址？
过程返回可以看成是执行一个特殊的OPR运算
注意：层次差为调用层次与定义层次的差值

    */
    private PerPcode IP;    //指令寄存器I，存放当前要执行的代码
    private int T;      //栈顶指示器T，指向数据栈STACK的栈，栈顶不存放元素？
    private int B;      //基址寄存器B，存放当前运行过程的数据区在STACK中的起始地址
    private int P;      //程序地址寄存器，存放下一条要执行的指令的地址
    private int stackSize=1000;
    private int stack_increment=100;
    private int max_stack_size=10000;
    private int[] dataStack=new int[stackSize];     //数据存储器STACK，初始值为1000
    private AllPcode code;            //存储器CODE，用来存放P的代码

    public void setPcode(AllPcode code){    //将Pcode中的代码用来初始化code
        this.code=code;
    }
    public AllPcode getCode(){
        return code;
    }
    public void interpreter(){
        P=0;
        B=0;
        T=0;
        IP=code.getPcodeArray()[P];
        do{
            IP=code.getPcodeArray()[P];
            P++;
            switch(IP.getF()){
                case 0:
                    dataStack[T]=IP.getA();
                    T++;
                    break;
                case 1:
                    switch(IP.getA()){
                        case 0:                 //opr,0,0此处不懂
                            T=B;
                            P=dataStack[B+2];
                            B=dataStack[B];
                            break;
                        case 1:                 //opr 0,1取反指令
                            dataStack[T-1]=-dataStack[T-1];
                            break;
                        case 2:                 //opr0 ,2相加，将原来的两个元素退去，将结果置于栈顶
                            dataStack[T-2]=dataStack[T-1]+dataStack[T-2];
                            T--;
                            break;
                        case 3:
                            dataStack[T-2]=dataStack[T-2]-dataStack[T-1];
                            T--;
                            break;
                        case 4:
                            dataStack[T-2]=dataStack[T-1]*dataStack[T-2];
                            T--;
                            break;
                        case 5:
                            dataStack[T-2]=dataStack[T-2]/dataStack[T-1];
                            T--;
                            break;
                        case 6:
                            dataStack[T-1]=dataStack[T-1]%2;//(奇数为1)
                            break;
                        case 7:
                            break;
                        case 8:
                            if (dataStack[T - 1] == dataStack[T-2]){//此处或使前面发生某些错误，因为Java中整型和boolean型是不可以等价的
                                dataStack[T-2]=1;
                                T--;
                                break;
                            }
                            dataStack[T-2]=0;
                            T--;
                            break;
                        case 9:
                            if (dataStack[T - 1] != dataStack[T-2]){//此处或使前面发生某些错误，因为Java中整型和boolean型是不可以等价的
                                dataStack[T-2]=1;
                                T--;
                                break;
                            }
                            dataStack[T-2]=0;
                            T--;
                            break;
                        case 10:
                            if(dataStack[T-2]<dataStack[T-1]){
                                dataStack[T-2]=1;
                                T--;
                                break;
                            }
                            dataStack[T-2]=0;
                            T--;
                            break;
                        case 11:
                            if(dataStack[T-2]>=dataStack[T-1]){
                                dataStack[T-2]=1;
                                T--;
                                break;
                            }
                            dataStack[T-2]=0;
                            T--;
                            break;
                        case 12:
                            if(dataStack[T-2]>dataStack[T-1]){
                                dataStack[T-2]=1;
                                T--;
                                break;
                            }
                            dataStack[T-2]=0;
                            T--;
                            break;
                        case 13:
                            if(dataStack[T-2]<=dataStack[T-1]){
                                dataStack[T-2]=1;
                                T--;
                                break;
                            }
                            dataStack[T-2]=0;
                            T--;
                            break;
                        case 14:
                            System.out.print(dataStack[T-1]);
                            System.out.print("  ");     //便于观察，再输出一个空格
                            break;
                        case 15:
                            System.out.println();
                            break;
                        case 16:
                            Scanner s=new Scanner(System.in);
                            dataStack[T]=s.nextInt();//此处存疑
                            T++;
                            break;
                    }
                    break;
                case 2:         //LOD L ，a 取变量（相对地址为a，层差为L）放到数据栈的栈顶
                    dataStack[T]=dataStack[IP.getA()+getBase(B,IP.getL())];
                    T++;
                    break;
                case 3://STO L ，a 将数据栈栈顶的内容存入变量（相对地址为a，层次差为L）
                    dataStack[IP.getA()+getBase(B,IP.getL())]=dataStack[T-1];
                    T--;
                    break;
                case 4: //CAL L ，a 调用过程（转子指令）（入口地址为a，层次差为L）
                    dataStack[T]=B;
                    dataStack[T+1]=getBase(B,IP.getL());
                    dataStack[T+2]=P;
                    B=T;
                    P=IP.getA();
                    break;
                case 5:  //INT 0 ，a 数据栈栈顶指针增加a
                    T=T+IP.getA();
                    break;
                case 6://JMP 0 ，a无条件转移到地址为a的指令
                    P=IP.getA();
                    break;
                case 7: //JPC 0 ，a 条件转移指令，转移到地址为a的指令
                    if(dataStack[T-1]==0){
                        P=IP.getA();
                    }
                    break;
            }
        }while(P!=0);

    }
    private int getBase(int nowBp,int lev){
        int oldBp=nowBp;      //SL
        while(lev>0){
            oldBp=dataStack[oldBp+1];
            lev--;
        }
        return oldBp;
    }


}
