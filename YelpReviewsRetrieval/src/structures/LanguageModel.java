/**
 * 
 */
package structures;

import java.util.HashMap;

/**
 * @author hongning
 * Suggested structure for constructing N-gram language model
 */
public class LanguageModel {

	int m_N; // N-gram
	HashMap<String, Token> m_model; // sparse structure for storing the maximum likelihood estimation of LM with the seen N-grams
	LanguageModel m_reference; // pointer to the reference language model for smoothing purpose
	
	double m_lambda; // parameter for linear interpolation smoothing
	double m_delta; // parameter for absolute discount smoothing
	
	public LanguageModel(int N) {
		m_N = N;
		m_model = new HashMap<String, Token>();
	}
	
	public double calcMLProb(String token) {
		// return m_model.get(token).getValue(); // should be something like this
		return 0;
	}

	public double calcLinearSmoothedProb(String token) {
		if (m_N>1) 
			return (1.0-m_lambda) * calcMLProb(token) + m_lambda * m_reference.calcLinearSmoothedProb(token);
		else
			return 0; // please use additive smoothing to smooth a unigram language model
	}
}
