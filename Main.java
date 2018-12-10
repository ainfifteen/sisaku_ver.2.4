package sisaku;

import java.io.IOException;
import java.util.Random;

public class Main{
	public static void main(String args[]) throws IOException{

		double simTime = 1;//シミュレーション間隔 0.1
		double endTime = 400;//シミュレーション時間(招集しないなら406秒
		double lapseTime = 1;//経過時間

		//long sleepTime = (long) (simTime * 1000);

		Random rnd = new Random();
		int iValue = rnd.nextInt(2) + 6;

		int jValue = rnd.nextInt(8);

		int[][] field = new int[24][24];//データの格納
		for(int i = 0; i < 24; i++) {
			for(int j = 0; j < 24; j++) {
				field[i][j] = 0;
				//field[iValue][jValue] = 1;
				field[0][13] = 1;
				field[8][5] = 1;
			}
		}

		int [][] divisionField = new int[36][36];//データの格納
		for(int i = 0; i < 36; i++) {
			for(int j = 0; j < 36; j++) {
				divisionField[i][j] = 1;
			}
		}


		int[][] area = new int[9][2];//エリア生成
		int x = 0, y = 0;
		for(int i = 0; i < 9; i++) {
			if(i == 0) {
				x = 15;
				y = 225;

			}
			if(i == 3 || i == 6) {
				x = 15;
				y += 240;

			}
			area[i][0] = x;
			area[i][1] = y;
			x += 240;

		}


		Drone[] drone = new Drone[9];//ドローン9台生成


		for(int i = 0; i < 9; i++) {//ドローンに値を割り当て
			drone[i] = new Drone(i + 50001, area[i][0], area[i][1]);
		}

		EdgeServer edgeServer = new EdgeServer();//インスタンス生成

		while(lapseTime < endTime) {

			for(int i = 0; i < 9; i++) {
				drone[i].move(simTime);
				drone[i].dataGet(field);
				drone[i].gDataGet(divisionField);

				System.out.println("ドローン"+(i+1)+":状態"+ drone[i].state+" x:"+ drone[i].x+"  y:"+drone[i].y+
						" 方向:"+drone[i].direction + " "+ drone[i].battery+" 招集状態"+drone[i].gatheringState +
						" " );

				edgeServer.receiveData(area[i][0], area[i][1], simTime);

				/*if(drone[i].time.equals("sensingstart")) {
					String str =  String.valueOf(lapseTime) + " " + String.valueOf(drone[i].id);
					try {//ファイルへの書き込み
						FileWriter fw = new FileWriter("/Users/TKLab/Desktop/sennsingStartTime.txt",true);
						BufferedWriter bw = new BufferedWriter(fw);
						bw.write(str);
						bw.newLine();//改行
						bw.flush();
						bw.close();//ファイル閉鎖
					}catch(IOException e) {
						System.out.println("エラー");
					}

				}

				if(drone[i].time.equals("sensingEnd")) {
					String str =  String.valueOf(lapseTime) + " " + String.valueOf(drone[i].id);
					try {//ファイルへの書き込み
						FileWriter fw = new FileWriter("/Users/TKLab/Desktop/sennsingEndTime.txt",true);
						BufferedWriter bw = new BufferedWriter(fw);
						bw.write(str);
						bw.newLine();//改行
						bw.flush();
						bw.close();//ファイル閉鎖
					}catch(IOException e) {
						System.out.println("エラー");
					}

				}

				if(drone[i].time.equals("missionEnd")) {
					String str =  String.valueOf(lapseTime) + " " + String.valueOf(drone[i].id);
					try {//ファイルへの書き込み
						FileWriter fw = new FileWriter("/Users/TKLab/Desktop/missionEndTime.txt",true);
						BufferedWriter bw = new BufferedWriter(fw);
						bw.write(str);
						bw.newLine();//改行
						bw.flush();
						bw.close();//ファイル閉鎖
					}catch(IOException e) {
						System.out.println("エラー");
					}

				}

				if(drone[i].message.equals("end")) {
					String str =  String.valueOf(lapseTime) + " " + String.valueOf(drone[i].id);
					try {//ファイルへの書き込み
						FileWriter fw = new FileWriter("/Users/TKLab/Desktop/data.txt",true);
						BufferedWriter bw = new BufferedWriter(fw);
						bw.write(str);
						bw.newLine();//改行
						bw.flush();
						bw.close();//ファイル閉鎖
					}catch(IOException e) {
						System.out.println("エラー");
					}

				}*/
				System.out.println("");
			}



			lapseTime += simTime;
			System.out.println("経過時間："+lapseTime);

			/*try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}*/
		}


	}
}