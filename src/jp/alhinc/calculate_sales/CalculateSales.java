package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// 売上データ集計処理
		if(!sumSales(args[0], branchSales)) {
			return;
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

		System.out.println("正常終了");

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
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			String splitLines[];
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				//カンマ区切りで取得
				splitLines = line.split(",");
				//支店コードと支店名をセットで取得
				branchNames.put(splitLines[0], splitLines[1]);
				branchSales.put(splitLines[0], 0L);
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
	 * 支店別売上集計処理
	 *
	 * @param フォルダパス
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean sumSales(String path, Map<String, Long> branchSales) {
		File[] files = new File(path).listFiles();
		String fileNameRegex = "^[0-9]{8}.rcd$";

		//集計ファイルフォルダ内のデータで繰り返し
		for(File file : files) {
			//売上ファイルかチェック
			if(file.getName().matches(fileNameRegex)){
				BufferedReader br = null;
				ArrayList<String> rcds = new ArrayList<String>();
				//1行目：支店コード、　2行目：売上金額

				try {
					FileReader fr = new FileReader(file);
					br = new BufferedReader(fr);
					String branchCode;
					String saleAmount;

					// 支店コード読み込み
					branchCode = br.readLine();

					//存在する支店コードかチェック
					if(branchSales.containsKey(branchCode)) {
						// 売上金額読み込み
						saleAmount = br.readLine();

						// 売上ファイル集計
						branchSales.replace(
							branchCode,
							branchSales.get(branchCode) + Long.parseLong(saleAmount)
						);
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
				String branchCode;
				String branchName;
				Long saleAmount;

				//情報取得
				branchCode = key;
				branchName = branchNames.get(key);
				saleAmount = branchSales.get(key);

				//書き込み
				bw.write(branchCode + "," + branchName + "," + saleAmount);
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
