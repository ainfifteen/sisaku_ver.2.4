package sisaku;


//このプログラムは,マルチキャストを使ってチャット機能を提供します
//ライブラリの利用

import java.io.BufferedReader;//文字、配列、行をバッファリングすることによって、
//文字型入力ストリームからテキストを効率良く読み込み
import java.io.InputStreamReader;//バイト・ストリームから文字ストリームへの橋渡しをする
//バイトを読み込み、指定されたcharsetを使用して文字にデコード
import java.net.DatagramPacket;//データグラムパケットを表す
//データグラムパケットは、無接続パケット配布サービスを実装する際に使用
//各メッセージは、パケット内に含まれている情報だけを基にマシンからマシンへ送信される
import java.net.InetAddress;//IPアドレスを表すクラス
import java.net.MulticastSocket;//IP マルチキャストパケットを送受信する

//受信スレッドを作成・実行し,送信を担当
public class Udp {
	final byte TTL = 1 ;//同一セグメント内部のみ到達可能とする ローカル
	String MULTICASTADDRESS;
				// マルチキャストアドレス224.0.0.1は,
				// ルータを超えない場合のアドレスです
	int port;
	byte[] buff = new byte[1024] ;//送信用バッファ 配列の定義
	MulticastSocket soc = null ; // マルチキャストソケット
	InetAddress droneGroup = null ; //チャット用アドレス

	// コンストラクタ
	public Udp(int portno, String mcast){
	port = portno ; //ポート番号の設定
	MULTICASTADDRESS = mcast;
	BufferedReader lineread
	  = new BufferedReader(new InputStreamReader(System.in)) ;

	}

	// makeMulticastSocketメソッド
	//MULTICASTADDRESSに対してマルチキャストソケットを作成
	public void makeMulticastSocket()
	{
		try{
			droneGroup = InetAddress.getByName(MULTICASTADDRESS) ;//指定されたホスト名を持つホストの IP アドレスを取得
			soc = new MulticastSocket(port) ;//特定のポートにバインドされたマルチキャストソケットを作成
			soc.joinGroup(droneGroup) ;//あるマルチキャストグループに参加
		}
		catch(Exception e){//例外処理
			e.printStackTrace() ;//エラーの説明
			System.exit(1);//異常終了
		}
	}

	// startLintenerメソッド
	// スレッド用クラスListenPacketのオブジェクトを生成し,起動
	ListenPacket lisner;
	public void startListener()
	{
		try{
			lisner = new ListenPacket(soc);
			Thread lisner_thread = new Thread(lisner);
			lisner_thread.start();//受信スレッドの開始(スレッドの実行を開始します。Java仮想マシンは、このスレッドのrunメソッドを呼び出す
		}
		catch(Exception e){
			e.printStackTrace() ;
			System.exit(1);
		}
	}

	// sendMsgsメソッド
	// マルチキャストパケットの送信を担当
	public void sendData(int dport, String message, int people, double x, double y, double battery, int port)
	{
		try{
				String str =  "ドローン " + String.valueOf(dport) + " ： " +
								String.valueOf(people) + " 人 " + "x： " +
								String.valueOf(x) + " " + "y： " +
								String.valueOf(y) + " " + "バッテリ： " +
								String.valueOf(battery) + " "+
								String.valueOf(message) + " ";
				buff = str.getBytes();//文字列をバイト化
				DatagramPacket dp
					= new DatagramPacket(buff,buff.length,droneGroup,port) ;//(パケットデータ,パケットの長さ,転送先アドレス,転送先ポート番号)
				soc.send(dp) ;
		}
		catch(Exception e){
			e.printStackTrace() ;
			System.exit(1);
		}
	}

	public void sendData(int dport,double x, double y, double battery, int state, int port)
	{
		try{
				String str =  "ドローン " + String.valueOf(dport) + " ： " + "x： " +
								String.valueOf(x) + " " + "y： " +
								String.valueOf(y) + " " + "バッテリ " +
								String.valueOf(battery)+" "+
								String.valueOf(state) + " ";
				buff = str.getBytes();//文字列をバイト化
				DatagramPacket dp
					= new DatagramPacket(buff,buff.length,droneGroup,port) ;//(パケットデータ,パケットの長さ,転送先アドレス,転送先ポート番号)
				soc.send(dp) ;
		}
		catch(Exception e){
			e.printStackTrace() ;
			System.exit(1);
		}
	}

	// sendMsgFromServerメソッド
	// エッジサーバからのマルチキャストパケットの送信を担当
	public void sendMsgsFromServer(int port, String judg)//エッジサーバからの送信
	{
		try{
				String str = String.valueOf(judg);
				buff = str.getBytes();
				DatagramPacket dp
					= new DatagramPacket(buff,buff.length,droneGroup,port) ;
				soc.send(dp) ;
		}
		catch(Exception e){
			e.printStackTrace() ;
			System.exit(1);
		}
	}

	public void sendMsgsFromServer(int port, double x, double y, String message)//エッジサーバからの送信
	{
		try{
				String str = "x： " + String.valueOf(x) + " " + "y： " +
							  String.valueOf(y) + " " +
							  String.valueOf(message) + " ";
				buff = str.getBytes();
				DatagramPacket dp
					= new DatagramPacket(buff,buff.length,droneGroup,port) ;
				soc.send(dp) ;
				//System.out.println(str);
		}
		catch(Exception e){
			e.printStackTrace() ;
			System.exit(1);
		}
	}

	// quitGroupメソッド
	// 接続を終了します
	public void quitGroup()
	{
		try{
			// 接続の終了
			System.out.println("接続終了") ;
			soc.leaveGroup(droneGroup) ;
			System.exit(0) ;//プログラムの終了
		}
		catch(Exception e){
			e.printStackTrace() ;
			System.exit(1);
		}
	}

}

//ListenPacketクラス
//マルチキャストパケットを受信
class ListenPacket implements Runnable {
	MulticastSocket s = null;//初期化
	// コンストラクタマルチキャストスレッド受け取り
	public ListenPacket(MulticastSocket soc){
		s = soc;
	}

	byte[] rcvData;

	// 処理の本体
	public void run(){
		byte[] buff = new byte[1024] ;
		try{
			while(true){
				DatagramPacket recv
					= new DatagramPacket(buff,buff.length) ;
				s.receive(recv) ;
				//s.setSoTimeout( 5 );
				//System.out.write(buff,0,recv.getLength()) ;
				this.rcvData = buff;
			}
		}catch(Exception e){
			e.printStackTrace() ;
			System.exit(1) ;
		}
	}
	public byte[] getData() {return rcvData;}
	public void resetData() { rcvData = null;}//データリセット

}