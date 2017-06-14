
//to perform Term at a time and Document at a time boolean query processing

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.LinkedList;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.Terms;

public class IR_project2 {
	// Hashmap structure to store terms with their postings
	static HashMap<String, LinkedList<Integer>> mainmap = new HashMap<String, LinkedList<Integer>>();
	static String indexPath, ipPath, opPath;

	public static void main(String[] args) throws IOException {

		indexPath = args[0]; // to retrieve and store index path
		opPath = args[1]; // to direct and write output to output.txt file
		ipPath = args[2]; // to retrieve query terms from input.txt file
		try {
			FileSystem file = FileSystems.getDefault();
			Directory d = FSDirectory.open(file.getPath(indexPath, new String[0]));
			IndexReader indexread = DirectoryReader.open(d);
			Fields f = MultiFields.getFields(indexread);
			String[] different_lang = new String[] { "text_nl", "text_fr", "text_de", "text_ja", "text_ru", "text_pt",
					"text_es", "text_es", "text_it", "text_da", "text_no", "text_sv" };
			int j;
			for (j = 0; j < 12; j++) {
				Terms term = f.terms(different_lang[j]);
				TermsEnum te = term.iterator();

				BytesRef bf;
				while ((bf = te.next()) != null) {
					PostingsEnum pe = MultiFields.getTermDocsEnum(indexread, different_lang[j], bf);
					LinkedList<Integer> id = new LinkedList<Integer>();
					mainmap.put(bf.utf8ToString(), id);
					while (pe.nextDoc() != pe.NO_MORE_DOCS) {

						id.add(pe.docID());
					}

				}

			}

			FileReader fileReader = new FileReader(ipPath);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			PrintStream output = new PrintStream(new FileOutputStream(opPath));
			while ((line = bufferedReader.readLine()) != null) {

				String[] queryTerms = line.split(" ");

				for (int i = 0; i < queryTerms.length; i++) {
					GetPostings(queryTerms[i], output);
					System.out.println(queryTerms[i]);
				}
				termAtATimeOr(queryTerms, output);
				termAtATimeAnd(queryTerms, output);
				documentAtATimeAnd(queryTerms, output);
				documentAtATimeOr(queryTerms, output);
			}
			output.close();

		}

		catch (Exception e) {
			e.printStackTrace();
			System.out.print("error" + e);
		}

	}

	/*
	 * method to extract the postings of query terms extract the postings from
	 * hashmap and print the postings of corresponding terms
	 */
	static void GetPostings(String query, PrintStream output) throws IOException {

		output.println("GetPostings" + " ");
		output.println(query);
		output.print("Postings List: " + " ");
		int temp[] = new int[mainmap.get(query).size()];
		int len = mainmap.get(query).size();
		for (int i = 0; i < len; i++) {
			temp[i] = mainmap.get(query).get(i);
			output.print(temp[i] + " ");

		}
		output.println();

	}

	/*
	 * calculate Document At A Time for Boolean-OR query processing extract the
	 * query terms and their postings perform union of the postings of all the
	 * terms in one sentence by traversing the postings lists in parallel
	 */
	public static void documentAtATimeOr(String[] query, PrintStream output) throws IOException {
		output.println("DaatOr");
		int comparisons = 0, len = query.length;
		int i = 0;
		while (i < len) {
			output.print(query[i] + " ");// retrieve the query terms in one
											// sentence of the input file
			i++;
		}

		LinkedList<Integer>[] daat_OR = new LinkedList[len];// creating a linked
															// list of arrays of
															// the postings for
															// the query terms
		LinkedList<Integer> postings_daat = new LinkedList<Integer>();// creating
																		// a
																		// linked
																		// list
																		// to
																		// store
																		// the
																		// final
																		// union
																		// of
																		// all
																		// the
																		// postings
																		// in
																		// one
		int pointer[] = new int[len];
		/*
		 * adding postings to the linked list of arrays assigning pointer of
		 * each query term to zero in linked list array
		 */
		for (int j = 0; j < len; j++) {
			daat_OR[j] = mainmap.get(query[j]);

			pointer[j] = 0;
		}
		/*
		 * run an infinite while loop to perform comparisons with the comparator
		 * value this gives us the union of postings in sorted order without any
		 * duplication
		 */
		while (0 != 1) {
			int counter = 0;
			int comparator_value = 46000;
			for (int i1 = 0; i1 < len; i1++) {
				if (!(pointer[i1] >= daat_OR[i1].size())) {
					if (daat_OR[i1].get(pointer[i1]) < comparator_value) {
						comparator_value = daat_OR[i1].get(pointer[i1]);

					}
				}

				else {
					counter++;
				}

			}

			if (counter == len) {
				break;
			}

			for (int k = 0; k < len; k++) {
				if (!(pointer[k] >= daat_OR[k].size())) {
					comparisons++;
					if (daat_OR[k].get(pointer[k]) == comparator_value) {
						pointer[k]++;
						counter++;

					}
				}

			}
			postings_daat.add(comparator_value);

		}
		output.println();

		output.print("Results:");
		for (int l = 0; l < postings_daat.size(); l++) {
			output.print(postings_daat.get(l) + " ");

		}
		output.println();

		output.println("Number of documents in result: " + postings_daat.size());
		output.println("Number of comparisons:" + comparisons);
	}

	/*
	 * method to perform Document at a Time for Boolean-AND query processing
	 * this method traverses the linked lists of posting lists of query terms in
	 * parallel and return the intersection of the lists i.e. the common doc IDs
	 * present in the posting lists of all query terms
	 */
	public static void documentAtATimeAnd(String[] query, PrintStream output) throws IOException {
		output.println("DaatAnd");
		int comparisons = 0, len = query.length;
		int i = 0;
		while (i < len) {
			output.print(query[i] + " ");
			i++;
		}
		int counter = 0;

		LinkedList<Integer>[] comparator = new LinkedList[len];// creating an
																// linked list
																// of arrays of
																// the postings
																// for the query
																// terms
		LinkedList<Integer> postings_daat = new LinkedList<Integer>();
		int pointer[] = new int[len];
		/*
		 * adding postings to the linked list of arrays assigning pointer of
		 * each query term to zero in linked list array
		 */
		for (int j = 0; j < len; j++) {
			comparator[j] = mainmap.get(query[j]);

			pointer[j] = 0;
		}

		while (true) {
			int comparator_value = 46000;
			for (int i1 = 0; i1 < len; i1++) {
				if (pointer[i1] >= comparator[i1].size()) {
					counter = 1;
					break;
				}
				if (comparator[i1].get(pointer[i1]) < comparator_value) {
					comparator_value = comparator[i1].get(pointer[i1]);

				}

			}
			// processing terminates once either of the posting lists get
			// exhausted

			if (counter == 1) {
				break;
			}
			int c = 0;
			for (int k = 0; k < len; k++) {
				if (comparator[k].get(pointer[k]) == comparator_value) {
					pointer[k]++;
					c++;
					comparisons++;
				}
				if (c == len) {
					postings_daat.add(comparator_value);
				}
			}

		}
		output.println();

		if (postings_daat.size() == 0) {
			output.println("Results: empty");
		} else {
			output.print("Results:");
			for (int i1 = 0; i1 < postings_daat.size(); i1++) {
				output.print(postings_daat.get(i1) + " ");

			}
			output.println();
		}
		output.println("Number of documents in result: " + postings_daat.size());
		output.println("Number of comparisons:" + comparisons);
	}

	/*
	 * method to perform Term At A Time Boolean-OR processing here the union of
	 * the postings of all query terms is computed by performing union of the
	 * postings list by traversing one list at a time
	 */
	public static void termAtATimeOr(String[] query, PrintStream output) throws IOException {
		output.println("TaatOr");
		int comparisons = 0;
		LinkedList<Integer>[] taat_OR = new LinkedList[query.length];
		int i;
		for (i = 0; i < query.length; i++) {
			taat_OR[i] = mainmap.get(query[i]);

		}

		int k;

		for (k = 0; k < taat_OR.length - 1; k++) {
			LinkedList<Integer> temp1 = new LinkedList<Integer>();
			int ptr1 = 0, ptr2 = 0;
			while (ptr1 < taat_OR[k].size() && ptr2 < taat_OR[k + 1].size()) {

				if (taat_OR[k].get(ptr1).equals((taat_OR[k + 1].get(ptr2)))) {
					temp1.add(taat_OR[k].get(ptr1));
					ptr1++;
					ptr2++;
					comparisons++;
				} else if (taat_OR[k].get(ptr1) < taat_OR[k + 1].get(ptr2)) {
					temp1.add(taat_OR[k].get(ptr1));
					ptr1++;
					comparisons++;
				} else {
					temp1.add(taat_OR[k + 1].get(ptr2));
					ptr2++;
					comparisons++;
				}

			}
			while (ptr1 < taat_OR[k].size()) {
				temp1.add(taat_OR[k].get(ptr1));
				ptr1++;

			}
			while (ptr2 < taat_OR[k + 1].size()) {
				temp1.add(taat_OR[k + 1].get(ptr2));
				ptr2++;

			}

			taat_OR[k + 1] = temp1;

		}
		for (int j = 0; j < query.length; j++) {
			output.print(query[j] + " ");
		}
		output.println();
		int length = taat_OR[(taat_OR.length) - 1].size();
		output.print("Results: ");
		for (int p = 0; p < (taat_OR[(taat_OR.length) - 1].size()); p++) {
			output.print(taat_OR[(taat_OR.length) - 1].get(p) + " ");
		}
		output.println();
		output.println("\n" + "Number of documents in results:" + (taat_OR[(taat_OR.length) - 1].size()));
		output.println("No.of Comparisons:" + comparisons);
	}

	/*
	 * method to perform Term At A Time Boolean-AND processing here the union of
	 * the postings of all query terms is computed by finding the common
	 * postings list by traversing one list at a time
	 */
	public static void termAtATimeAnd(String[] query, PrintStream output) throws IOException {
		output.println("TaatAnd");
		int comparisons = 0;
		LinkedList<Integer>[] taat_AND = new LinkedList[query.length];
		for (int i = 0; i < query.length; i++) {
			taat_AND[i] = mainmap.get(query[i]);
		}

		for (int k = 0; k < taat_AND.length - 1; k++) {
			LinkedList<Integer> temp1 = new LinkedList<Integer>();
			int ptr1 = 0, ptr2 = 0;
			while (ptr1 < taat_AND[k].size() && ptr2 < taat_AND[k + 1].size()) {

				if (taat_AND[k].get(ptr1).equals((taat_AND[k + 1].get(ptr2)))) {
					temp1.add(taat_AND[k].get(ptr1));
					ptr1++;
					ptr2++;
					comparisons++;
				} else if (taat_AND[k].get(ptr1) < taat_AND[k + 1].get(ptr2)) {

					ptr1++;
					comparisons++;
				} else {

					ptr2++;
					comparisons++;
				}
			}
			taat_AND[k + 1] = temp1;

		}
		for (int i = 0; i < query.length; i++) {
			output.print(query[i] + " ");
		}
		output.println();
		int length = taat_AND[(taat_AND.length) - 1].size();
		if (taat_AND[(taat_AND.length) - 1].size() == 0) {
			output.print("Result:" + "empty");
		} else {
			output.print("Result:" + " ");

			for (int p = 0; p < (taat_AND[(taat_AND.length) - 1].size()); p++) {
				output.print(taat_AND[(taat_AND.length) - 1].get(p) + " ");
			}
		}
		output.println();
		output.println("\n" + "Number of documents in results:" + (taat_AND[(taat_AND.length) - 1].size()));
		output.println("No.of Comparisons:" + comparisons);
	}

}
