package sisaku;

public class Drone {

	final static int EDGE_SERVER = 60000;

	final static int WAIT = 0;//初期
	final static int GO = 1;//移動
	final static int SENSING = 2;//通常状態
	final static int GATHERING = 3;//招集
	final static int BACK = 4;//帰還
	final static int END = 5;//終了

	final static short NULL = 0;
	final static short NORTH = 1;
	final static short EAST = 2;
	final static short SOUTH = 3;
	final static short WEST = 4;

	final static int gNULL = 0;
	final static int gWAIT = 1;
	final static int gSTANDBY = 2;
	final static int gSENSING = 3;
	final static int gBACK = 4;

	final static int gDNULL = 0;
	final static short gNORTH = 1;
	final static short gEAST = 2;
	final static short gSOUTH = 3;
	final static short gWEST = 4;

	final static double CONSUMPTION = 0.06;//1秒間の電池消費

	int id;//ドローンのID
	double x,y,z;
	double battery;//バッテリー
	int state;//ドローンの状態
	int gatheringState;
	int initState;
	int initX, initY;;//初期値
	double gInitX;
	double gInitY;
	double meetingPlaceX;
	double meetingPlaceY;
	int oneBlock;//1区画
	int gOneBlock;
	int discover;//データの格納
	int gDiscover;
	String message;
	String time;

	short direction;//方向
	short gDirection;//招集時の方向
	double speed;//スピード
	double firstMove;//初期移動
	double firstGatheringMove;
	double endGatheringMove;
	double arrivalTime;//到着時間
	double arrivalGatheringTime;
	double endGatheringTime;
	double lapseTime;//経過時間

	String meetingPlace[] = new String [2];

	Udp udp;
	Udp udp2;
	Udp udp3;


	Drone(int id, int initX, int initY){//コンストラクタ
		this.id = id;
		this.initX = initX;
		this.initY = initY;
		x = 0.0;
		y = 0.0;
		battery = 100.0;
		lapseTime = 0.0;
		state = WAIT;
		gatheringState = gNULL;
		direction = NULL;
		gDirection = NULL;
		speed = 10;
		oneBlock = 30;
		gOneBlock = 20;
		firstMove = Math.sqrt(Math.pow(initX - x, 2) + Math.pow(initY - y, 2));
		arrivalTime = firstMove / speed;
		message = "Normal ";
		time = " ";

		udp = new Udp(id, "224.0.0.2");
		udp.makeMulticastSocket() ;//ソケット生成
		udp.startListener() ;//受信

		udp2 = new Udp(id, "224.0.0.3");
		udp2.makeMulticastSocket() ;//ソケット生成
		udp2.startListener() ;//受信

		udp3 = new Udp(id, "224.0.0.4");
		udp3.makeMulticastSocket() ;//ソケット生成
		udp3.startListener() ;
	}

	void move(double simTime) {//移動メソッド

		lapseTime += simTime;


		if(state != END) {
			battery -= CONSUMPTION * simTime;
		}

		if(battery < 10.0) state = BACK;//10%以下で帰還


		udp2.sendData(id, x, y, battery, state, EDGE_SERVER);
		udp2.lisner.resetData();



		switch(state) {

		case WAIT:
			 state = GO;
			 break;

		case GO:
			message = "Normal ";
			convenerRecieveData();
			double goTheta = Math.atan2(initY, initX);//角度
			double goDistance = speed * simTime;
			x += goDistance * Math.cos(goTheta);
			y += goDistance * Math.sin(goTheta);


			if(lapseTime >= arrivalTime){
				x = initX;
				y = initY;
				time = "sensingstart";
				direction = SOUTH;
				lapseTime = 0.0;
				state = SENSING;
			}
			break;

		case SENSING:
			time = " ";
			message = "Normal ";
			convenerRecieveData();
			if(lapseTime >= oneBlock / speed) {
				switch(direction) {
				case NORTH://上へ
					y += oneBlock;
					if(y >= initY) direction = EAST;
					break;

				case EAST://右へ
					x += oneBlock;
					if(y >= initY) direction = SOUTH;
					else direction = NORTH;
					break;

				case SOUTH://下へ
					y -= oneBlock;
					if(y <= initY - 210) direction = EAST;
					break;

				case WEST: break;//左へ
				default: break;

				}

				judgRecieveData();

			    lapseTime = 0.0;//経過時間

			}

			if(x >= initX + 210 && y >= initY) {
				time = "sensingEnd";
				state = BACK;
				direction = NULL;
			}

			break;

		case GATHERING:
			switch(gatheringState) {
			case gWAIT:
				byte[] rcvDataThird = udp3.lisner.getData();
				if(rcvDataThird != null) {//受信データが空でないのなら
					String str3 = new String(rcvDataThird,0,33);//byte型を文字に変換(ごみを削除)
					String[] convenedData = str3.split(" ", 0);//受信データの分割

					/*for (int i = 0 ; i < convenedData.length ; i++){
					      System.out.println(i + "番目の要素 = :" + convenedData[i]);
					}*/

					if(convenedData[4].equals("MainRequest")) {
						message = "MainReply ";
						meetingPlace[0] = convenedData[1];//召集先のX座標
						meetingPlace[1] = convenedData[3];//召集先のY座標
						lapseTime = 0.0;//経過時間
						gatheringState = gSTANDBY;
					}
					udp3.lisner.resetData();//データのリセット

				}
				break;

			case gSTANDBY:
				//System.out.println(meetingPlace[0] + " " + meetingPlace[1]);
				meetingPlaceX = Double.parseDouble(meetingPlace[0]);//召集先のX座標
				meetingPlaceY = Double.parseDouble(meetingPlace[1]);//召集先のY座標

				firstGatheringMove = Math.sqrt(Math.pow(meetingPlaceX - gInitX, 2) + Math.pow(meetingPlaceY - gInitY, 2));

				double gStandbyTheta = get_gStandbyTheta();
				double gStandbyDistance = speed * simTime;

				//System.out.println(gInitX + " " +gInitY);
				//System.out.println("相対距離" + firstGatheringMove);
				//System.out.println(gStandbyTheta);

				System.out.println("招集エリアに移動中");

				if(x > meetingPlaceX && y > meetingPlaceY) {
					x -= gStandbyDistance * Math.cos(gStandbyTheta);
					y -= gStandbyDistance * Math.sin(gStandbyTheta);
				}
				else if(x <= meetingPlaceX && y > meetingPlaceY) {
					x += gStandbyDistance * Math.cos(gStandbyTheta);
					y -= gStandbyDistance * Math.sin(gStandbyTheta);
				}
				else if(x > meetingPlaceX && y <= meetingPlaceY) {
					x -= gStandbyDistance * Math.cos(gStandbyTheta);
					y += gStandbyDistance * Math.sin(gStandbyTheta);
				}
				else{
					x += gStandbyDistance * Math.cos(gStandbyTheta);
					y += gStandbyDistance * Math.sin(gStandbyTheta);
				}

				arrivalGatheringTime = firstGatheringMove / speed;

				if(lapseTime >= arrivalGatheringTime){
					x = meetingPlaceX;
					y = meetingPlaceY;
					lapseTime = 0.0;
					gatheringState = gSENSING;
					gDirection = gSOUTH;
				}

				break;

			case gSENSING:
				//System.out.println("招集センシング中");
				message = "start";
				message = " ";
				if(lapseTime >= (gOneBlock / speed)) {
					switch(gDirection) {
					case gNORTH://上へ
						y += gOneBlock;
						if(y >= meetingPlaceY) gDirection = gEAST;
						break;

					case gEAST://右へ
						x += gOneBlock;
						if(y >= meetingPlaceY) gDirection = gSOUTH;
						else gDirection = gNORTH;
						break;

					case gSOUTH://下へ
						y -= gOneBlock;
						if(y <= meetingPlaceY - 100) gDirection = gEAST;
						break;

					case gWEST: break;//左へ
					default: break;

					}
					lapseTime = 0.0;//経過時間
				}

				if(x >= meetingPlaceX + 100 && y >= meetingPlaceY) {
					lapseTime = 0.0;
					gatheringState = gBACK;
					gDirection = gDNULL;
				}


				break;

			case gBACK:
				System.out.println("元の位置へ帰還中");
				double gBackTheta = get_gBackTheta();
				double gBackDistance = speed * simTime;
				System.out.println(gInitX + " " + gInitY);

				if(gInitX > meetingPlaceX + 120 && gInitY > meetingPlaceY) {
					x += gBackDistance * Math.cos(gBackTheta);
					y += gBackDistance * Math.sin(gBackTheta);
				}
				else if(gInitX <= meetingPlaceX + 120 && gInitY > meetingPlaceY) {
					x -= gBackDistance * Math.cos(gBackTheta);
					y += gBackDistance * Math.sin(gBackTheta);
				}
				else if(gInitX > meetingPlaceX + 120 && gInitY <= meetingPlaceY) {
					x += gBackDistance * Math.cos(gBackTheta);
					y -= gBackDistance * Math.sin(gBackTheta);
				}
				else{//(gInitX <= meetingPlaceX + 120 && gInitY <= meetingPlaceY)
					x -= gBackDistance * Math.cos(gBackTheta);
					y -= gBackDistance * Math.sin(gBackTheta);
				}

				endGatheringMove = Math.sqrt(Math.pow(meetingPlaceX + 120 - gInitX, 2) + Math.pow(meetingPlaceY - gInitY, 2));

				endGatheringTime = endGatheringMove / speed;

				if(lapseTime >= endGatheringTime) {
					x = gInitX;
					y = gInitY;
					message = "end";
					state = initState;
					gatheringState = gNULL;
				}

				break;

			}

			break;
		case BACK:
			time = " ";
			message = " ";
			System.out.println("帰還中");
			convenerRecieveData();
			double backTheta = Math.atan2(y, x);
			double backDistance = speed * simTime;
			x -= backDistance * Math.cos(backTheta);
			y -= backDistance * Math.sin(backTheta);

			if(x <= 0 && y <= 0) {
				x = 0;
				y = 0;
				time = "missionEnd";
				state = END;
				direction = NULL;
			}
			break;
		case END:
			time = " ";

		default:
			break;
		}

	 }



	void dataGet(int[][] area){//データ収集メソッド
		if(state == SENSING || state == GATHERING ) {//通常状態もしくは招集状態
			if(!(gatheringState == gSTANDBY || gatheringState == gSENSING || gatheringState == gBACK)) {

					discover = area[(int)(x / 30) ][(int)(y / 30) ];//データ抽出

					udp.sendData(id, message, discover, x, y, battery, EDGE_SERVER);//エッジに送信
					udp.lisner.resetData();//データのリセット
			}

		}
	}

	void gDataGet(int[][] divisionArea){//招集時データ収集メソッド
		if(state == GATHERING) {
			if(gatheringState == gSENSING) {
				message = "Convocation";
				/*if(x < gOneBlock && y < gOneBlock) {

				}
				else if(x == gOneBlock && y == gOneBlock) {*/
					gDiscover = divisionArea[(int)(x / gOneBlock) ][(int)(y / gOneBlock) ];//データ抽出
				/*}
				else if(x < gOneBlock && y == gOneBlock) {
					gDiscover = divisionArea[(int)(x / gOneBlock) ][(int)(y / gOneBlock) - 1];//データ抽出
				}
				else if(x <= gOneBlock && y > gOneBlock) {
					gDiscover = divisionArea[(int)(x / gOneBlock)][(int)(y / gOneBlock) - 1 ];//データ抽出
				}
				else if(x > gOneBlock && y <= gOneBlock) {
					gDiscover = divisionArea[(int)(x / gOneBlock) - 1][(int)(y / gOneBlock) ];//データ抽出
				}
				else if(x == gOneBlock && y < gOneBlock) {
					gDiscover = divisionArea[(int)(x / gOneBlock)][(int)(y / gOneBlock) ];//データ抽出
				}
				else {
					gDiscover = divisionArea[(int)(x / gOneBlock) - 1][(int)(y / gOneBlock) - 1];//データ抽出
				}*/
				udp3.sendData(id, message, gDiscover, x, y, battery, EDGE_SERVER);//エッジに送信
				udp3.lisner.resetData();

			}

		}

	}

	void judgRecieveData() {
		byte[] rcvData = udp.lisner.getData();
		if(rcvData != null) {//受信データが空でないのなら
			String str = new String(rcvData,0,1);//byte型を文字に変換(ごみを削除)
			//System.out.println(str);

			if(str.equals("T")) {
				if(battery >= 50.0) {
					message = "Accept ";
					udp.lisner.resetData();
					gInitX = x;
					gInitY = y;
					initState = state;
					state = GATHERING;//招集状態へ
					gatheringState = gWAIT;
				}
				else {
					message = "Decline ";
					udp.lisner.resetData();//データのリセット
					state = SENSING;//続行
					gatheringState = gNULL;
					}
				}

			else {
				state = SENSING;//続行
			}

			udp.lisner.resetData();//データのリセット
		}
	}

	void convenerRecieveData() {
		byte[] rcvDataThird = udp3.lisner.getData();
		if(rcvDataThird != null) {
			String str3 = new String(rcvDataThird,0,33);
			String[] convenedData = str3.split(" ", 0);//受信データの分割
			/*for (int i = 0 ; i < convenedData.length ; i++){
			      System.out.println(i + "番目の要素 = :" + convenedData[i]);
			}*/
			System.out.println(str3);
			if(convenedData[4].equals("MainRequest")) {
				message = "MainReply ";
				meetingPlace[0] = convenedData[1];//召集先のX軸
				meetingPlace[1] = convenedData[3];//召集先のY軸

				initState = state;

				gInitX = x;//招集直前のX座標
				gInitY = y;//招集直前のY座標

				lapseTime = 0.0;//経過時間
				state = GATHERING;//招集状態へ
				gatheringState = gSTANDBY;
			}
			udp3.lisner.resetData();
		}
	}

	double get_gStandbyTheta() {//召集の際の向かう場合の角度
		double gStandbyTheta;
		if(x > meetingPlaceX && y > meetingPlaceY) {
			gStandbyTheta = Math.atan2( y - meetingPlaceY, x - meetingPlaceX);//角度
		}
		else if(x <= meetingPlaceX && y > meetingPlaceY) {
			gStandbyTheta = Math.atan2( y - meetingPlaceY, meetingPlaceX - x);//角度
		}
		else if(x > meetingPlaceX && y <= meetingPlaceY) {
			gStandbyTheta = Math.atan2( meetingPlaceY - y, x - meetingPlaceX);//角度
		}
		else{
			gStandbyTheta = Math.atan2( meetingPlaceY - y, meetingPlaceX - x);//角度
		}
		return gStandbyTheta;
	}

	double get_gBackTheta() {//召集から帰ってくる場合の角度
		double gBackTheta;
		if(gInitX > (meetingPlaceX + 120) && gInitY > meetingPlaceY) {
			gBackTheta = Math.atan2( gInitY - meetingPlaceY, gInitX - (meetingPlaceX + 120));//角度
		}
		else if(gInitX <= (meetingPlaceX + 120) && gInitY > meetingPlaceY) {
			gBackTheta = Math.atan2( gInitY - meetingPlaceY, (meetingPlaceX + 120) - gInitX);//角度
		}
		else if(gInitX > (meetingPlaceX + 120) && gInitY <= meetingPlaceY) {
			gBackTheta = Math.atan2( meetingPlaceY - gInitY, gInitX - (meetingPlaceX + 120));//角度
		}
		else{//(gInitX <= meetingPlaceX + 120 && gInitY <= meetingPlaceY)
			gBackTheta = Math.atan2( meetingPlaceY - gInitY, (meetingPlaceX + 120) - gInitX);//角度
		}
		return gBackTheta;
	}
}