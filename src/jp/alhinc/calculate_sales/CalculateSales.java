package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SORTED = "売上ファイル名が連番になっていません";
	private static final String DATA_SALESUMMARY_OVER = "合計金額が10桁を超えました";
	private static final String DATA_INVALID_BRANCHCODE = "の支店コードが不正です";
	private static final String FILE_INVALID_SALESUMMARY = "のフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		//コマンドライン引数チェック
		if(args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();



		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}



		//集計処理
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<File>();
		String fileNameRegex = "^[0-9]{8}.rcd$";
		String saleNumberRegex = "^[0-9]+$";

		//集計ファイルフォルダ内のデータで繰り返し
		for(int i = 0; i < files.length; i++) {
			//売上ファイルかチェック
			if(files[i].getName().matches(fileNameRegex)){
				rcdFiles.add(files[i]);
			}
		}

		//売上ファイルリストのソート
		Collections.sort(rcdFiles);

		//売上ファイル名連番チェック
		for(int i = 0; i < rcdFiles.size()-1; i++) {
			//ファイル名取得
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i+1).getName().substring(0, 8));

			//連番チェック
			if((latter - former) != 1) {
				System.out.println(FILE_NOT_SORTED);
				return;
			}
		}

		//売上ファイルの数だけ繰り返し
		for(int i = 0; i < rcdFiles.size(); i++) {
			BufferedReader br = null;
			List<String> rcdDatas = new ArrayList<String>();

			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);
				String line;

				//ファイル内データ読み込み
				while((line = br.readLine()) != null) {
					//読み込み行保持
					rcdDatas.add(line);
				}

				//支店コード存在チェック
				if(!branchNames.containsKey(rcdDatas.get(0))) {
					System.out.println(files[i].getName() + DATA_INVALID_BRANCHCODE);
					return;
				}

				//売上値数値チェック
				if(!(rcdDatas.get(1).matches(saleNumberRegex) && (rcdDatas.get(1).length() <= 10))){
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				//フォーマットチェック
				if(rcdDatas.size() != 2) {
					System.out.println(files[i].getName() + FILE_INVALID_SALESUMMARY);
					return;
				}

				//売上値取得
				long fileSale = Long.parseLong(rcdDatas.get(1));

				//売上金額加算
				Long saleAmount = branchSales.get(rcdDatas.get(0)) + fileSale;

				//桁数チェック
				if(saleAmount >= 10000000000L){
					System.out.println(DATA_SALESUMMARY_OVER);
					return;
				}

				//売上金額更新
				branchSales.put(rcdDatas.get(0), saleAmount);
			} catch(IndexOutOfBoundsException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}



		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(
		String path, String fileName,
		Map<String, String> branchNames, Map<String, Long> branchSales
	) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			//ファイル存在確認
			if(!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			String items[];
			String formatRegex = "^[0-9]{3}+$";

			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				//カンマ区切りで取得
				items = line.split(",");

				//フォーマットチェック
				if(items.length != 2 || !items[0].matches(formatRegex)) {
					//フォーマット不正の場合
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				//支店コードと支店名をセットで取得
				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], 0L);
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(
		String path, String fileName,
		Map<String, String> branchNames, Map<String, Long> branchSales
	) {
		BufferedWriter bw = null;
		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			//書き込み処理
			for(String key : branchNames.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
