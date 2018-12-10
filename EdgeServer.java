package sisaku;
import java.io.IOException;

public class EdgeServer {
	final static int ID = 60000;;//エッジサーバのID
	double lapseTime;//経過時間
	String[] eachData;
	double[][] beforeConvocation = new double[9][4];
	double[] convenerCoordinate = new double[2];//招集元のXY座標
	double relativeDistance[][] = new double[9][2];
	int divisionArea[][] = new int [4][2];
	double dvisionAreaDistance[][][] = new double[2][4][4];
	int convocationOrder[][] = new int [4][3];//招集命令
	int state;


	String message;
	int initX, initY;;//初期O

	Udp udp;
	Udp udp2;
	Udp udp3;

	EdgeServer() throws IOException{
		udp = new Udp(ID, "224.0.0.2");//UDPインスタンスにID付与
		udp.makeMulticastSocket();//ソケット生成
		udp.startListener();//受信

		udp2 = new Udp(ID, "224.0.0.3");
		udp2.makeMulticastSocket() ;//ソケット生成
		udp2.startListener() ;

		udp3 = new Udp(ID, "224.0.0.4");
		udp3.makeMulticastSocket() ;//ソケット生成
		udp3.startListener() ;

	}

	void receiveData(int initX, int initY, double simTime) throws IOException{//受信メソッド
		this.initX = initX;
		this.initY = initY;
		byte[] rcvData = udp.lisner.getData();//受信データ
		byte[] rcvDataSecond = udp2.lisner.getData();
		byte[] rcvDataThird = udp3.lisner.getData();

		if(rcvDataSecond != null) {
			String str2 = new String(rcvDataSecond,0,110);
			String[] cData = str2.split(" ", 0);

			/*for (int i = 0 ; i < cData.length ; i++){
			      System.out.println(i + "番目の要素 = :" + cData[i]);
			}*/

			int dronePort = Integer.parseInt(cData[1]);
			double coordinateX = Double.parseDouble(cData[4]);//X座標
			double coordinateY = Double.parseDouble(cData[6]);//Y座標
			state = Integer.parseInt(cData[9]);
			double droneBattery = Double.parseDouble(cData[8]);

			for(int i = 0;i < 9; i++) {
				if(dronePort == 50001 + i) {//全てのドローンのパラメータ
					beforeConvocation[i][0] = dronePort;
					beforeConvocation[i][1] = coordinateX;//X軸
					beforeConvocation[i][2] = coordinateY;//Y軸
					beforeConvocation[i][3] = droneBattery;
				}
			}

		}
			/*for(int i = 0; i < 9; i++) {
				for(int j = 0; j < 4; j++) {
					System.out.println(beforeConvocation[i][j]);
				}

			}*/
		if(rcvDataThird != null) {
			String str3 = new String(rcvDataThird,0,121);
			udp3.lisner.resetData();
			System.out.println("(招集時の受信データ) " +str3);
			/*try {//ファイルへの書き込み
				FileWriter fw2 = new FileWriter("/Users/TKLab/Desktop/convenerData.txt",true);
				BufferedWriter bw2 = new BufferedWriter(fw2);
				bw2.write(str3);
				bw2.newLine();//改行
				bw2.flush();
				bw2.close();//ファイル閉鎖
			}catch(IOException e) {
				System.out.println("エラー");
			}*/
		}

		if(rcvData != null) {
			String str = new String(rcvData,0,121);//byte型から文字に変換
			udp.lisner.resetData();
			System.out.println("(エッジサーバ受信データ) "+str);
			eachData = str.split(" ", 0);//受信データの分割

			convenerCoordinate[0] = Double.parseDouble(eachData[6]);//招集元のX座標
			convenerCoordinate[1] = Double.parseDouble(eachData[8]);//招集元のY座標

			/*for (int i = 0 ; i < eachData.length ; i++){
			      System.out.println(i + "番目の要素 = :" + eachData[i]);
			}*/

			int dport = Integer.parseInt(eachData[1]);//ドローンのポート番号

			if((eachData[11].equals("Normal"))) {
				judgmentData(dport, eachData[3], simTime);//eachData[3]=discovery
			}

			if(!(eachData[11].equals("Normal")) && !(eachData[11].equals("Decline"))) {//プロトコルへ

				if(eachData[11].equals("Accept")) {
					udp.lisner.resetData();
					calcDistance();//相対距離の計算
					divisionArea(initX, initY);//該当エリアの分割
					calcAreaDistance();//
					for(int i = 0; i < 4; i ++) {
						message = "MainRequest";
						udp3.sendMsgsFromServer(convocationOrder[i][2], convocationOrder[i][0], convocationOrder[i][1], message);
						udp3.lisner.resetData();
						System.out.println("目標地点 " + convocationOrder[i][0] + " " + convocationOrder[i][1] + " " + convocationOrder[i][2]);
					}
				}

				if(eachData[11].equals("MainReply")) {
					message = "";

					udp.lisner.resetData();
				}

				if(eachData[11].equals("start")) {//特に書かないでいい

				}

				if(eachData[11].equals("end")) {
					message = "DissolutionRequest";
				}

			}

			/*try {//ファイルへの書き込み
				FileWriter fw = new FileWriter("/Users/TKLab/Desktop/data.txt",true);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(str);
				bw.newLine();//改行
				bw.flush();
				bw.close();//ファイル閉鎖
			}catch(IOException e) {
				System.out.println("エラー");
			}*/

			udp.lisner.resetData();//バッファの中のデータをリセット
		}

	}


	void  judgmentData(int dport,String peaple, double simTime){
		int human = Integer.parseInt(peaple);
		lapseTime += simTime;

		//if(lapseTime >= 0.3) {
			if(human != 0) {
				message = "T";
				udp.sendMsgsFromServer(dport,message);
			}
			else {
				message = "F";
				udp.sendMsgsFromServer(dport,message);
			}

			//lapseTime = 0;
		//}
	}


	void calcDistance() {
		if(state != 3) {
			for(int i = 0; i < 9; i++) {//相対距離の計算
				relativeDistance[i][0] = beforeConvocation[i][0];//ポート番号
				relativeDistance[i][1]
				= Math.sqrt(Math.pow(convenerCoordinate[0] - beforeConvocation[i][1], 2) + Math.pow(convenerCoordinate[1] - beforeConvocation[i][2], 2));

			}

			for(int i = 0; i < 9; i++) {//並び替え
				for(int j=0; j < 9-i-1; j++) {
					if(relativeDistance[j][1] >= relativeDistance[j + 1][1]) {
						double asc = relativeDistance[j][1];
						double port = relativeDistance[j][0];
						relativeDistance[j][1] = relativeDistance[j + 1][1];
						relativeDistance[j][0] = relativeDistance[j + 1][0];
						relativeDistance[j + 1][1] = asc;
						relativeDistance[j + 1][0] = port;
					}
				}
			}
		}

		/*for(int i = 0; i < 4; i++) {
				System.out.println(relativeDistance[i][0] + " " + relativeDistance[i][1]);
			}*/

	}


	void divisionArea(int x, int y) {//該当領域の分割
		int X = x, Y = y;
		for(int i = 0 ; i < 4 ; i++) {
			if(i % 2 == 1) {
				X += 120;
			}
			if(i == 2) {
				X = x;
				Y -= 120;
			}
			divisionArea[i][0] = X;
			divisionArea[i][1] = Y;
			//System.out.println("エリア" + (i+1) +" "+divisionArea[i][0]+ " " + divisionArea[i][1]);
		}
	}

	void calcAreaDistance() {
		for(int i = 0; i < 4; i++) {
			for(int j = 0; j < 9; j++) {
				if(relativeDistance[i][0] == beforeConvocation[j][0]) {
					for(int k = 0; k < 4; k ++) {
						dvisionAreaDistance[0][k][i] = relativeDistance[i][0];
						dvisionAreaDistance[1][k][i]
						= Math.sqrt(Math.pow(divisionArea[k][0] - beforeConvocation[j][1], 2) + Math.pow(divisionArea[k][1] - beforeConvocation[j][2], 2));
						//System.out.println(divisionArea[k][0]+" "+divisionArea[k][1]);
						//System.out.println(beforeConvocation[j][1]+" "+beforeConvocation[j][2]);

					}

				}

			}

		}
		int del_num[][] = {{1,1,1,1},{1,1,1,1},{1,1,1,1},{1,1,1,1}};
		double min[] = {10000,10000,10000,10000};
		int convocationPort [] = new int [4];
		int del_x = 0;
		int del_y = 0;

		for(int i = 0; i < 4; i++) {
			for(int k = 0; k < 4; k ++) {
				for(int j = 0; j < 4; j ++) {
					if(del_num[k][j] == 1) {
						if(min[i] > dvisionAreaDistance[1][k][j]) {

							min[i] = dvisionAreaDistance[1][k][j];
							convocationPort [i] = (int)dvisionAreaDistance[0][k][j];
							//System.out.println("エリア" + (k + 1) + " " +divisionArea[k][0]+ " " + divisionArea[k][1] + " " + relativeDistance[j][0] + "相対距離" + min[i]);

							del_x = k;
							del_y = j;
						}
					}
				}

		    }

			for(int k = 0; k < 4; k ++) {
				for(int j = 0; j < 4; j ++) {
					if(del_x==k||del_y==j) {
						del_num[k][j] = 0;
					}
				}
			}

		}

		for(int i = 0; i < 4; i ++) {
			convocationOrder[i][0] = divisionArea[i][0];
			convocationOrder[i][1] = divisionArea[i][1];
			convocationOrder[i][2] = convocationPort [i];
			//System.out.println(convocationOrder[i][0] + " " + convocationOrder[i][1] + " " + convocationOrder[i][2]);
		}
		System.out.println("");

		/*for(int i = 0; i < 4; i ++) {
			System.out.println(divisionArea[i][0]+ " " + divisionArea[i][1]+ " "+ convocationPort [i] + " 相対距離" + min[i]);

	    }*/

	}

}