package com.aether.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FinancePolicyTests {
    private FinancePolicy.Classification classify(String account,String description,String category,String amount){return FinancePolicy.classify(account,description,category,null,new BigDecimal(amount),null);}
    @Test void cardPurchaseAndBankPurchaseAreExpensesDespiteOppositeSourceSigns(){
        assertThat(classify("CREDIT","Mercado","Groceries","100").kind()).isEqualTo("EXPENSE");
        assertThat(classify("BANK","Mercado","Groceries","-100").kind()).isEqualTo("EXPENSE");
        assertThat(FinancePolicy.spending("EXPENSE",new BigDecimal("-100"))).isEqualByComparingTo("100");
    }
    @Test void bothSidesOfBillPaymentDoNotCountAsNewSpending(){
        assertThat(classify("BANK","PAGAMENTO FATURA INTER","Credit card payment","-100").kind()).isEqualTo("CARD_PAYMENT");
        assertThat(classify("CREDIT","Pagamento recebido","Credit card payment","-100").kind()).isEqualTo("CARD_PAYMENT");
        assertThat(FinancePolicy.spending("CARD_PAYMENT",new BigDecimal("-100"))).isZero();
    }
    @Test void refundsReduceSpendingAndAmbiguousCreditsNeedReview(){
        var refund=classify("CREDIT","Crédito","Shopping","-20");
        assertThat(refund.kind()).isEqualTo("REFUND");assertThat(refund.review()).isTrue();
        assertThat(FinancePolicy.spending(refund.kind(),new BigDecimal("-20"))).isEqualByComparingTo("-20");
    }
    @Test void transfersAndInvestmentsDoNotInflateIncomeOrSpending(){
        assertThat(classify("BANK","PIX","Same person transfer","500").kind()).isEqualTo("TRANSFER");
        assertThat(classify("BANK","CDB C6 LIM.GARANT.","Automatic investment","-450").kind()).isEqualTo("INVESTMENT");
        assertThat(classify("BANK","PIX recebido","Transfer - PIX","30").review()).isTrue();
    }
    @Test void explicitReviewWinsOverProviderSuggestions(){
        var reviewed=FinancePolicy.classify("BANK","PIX recebido","Transfer - PIX",null,new BigDecimal("30"),"TRANSFER");
        assertThat(reviewed.kind()).isEqualTo("TRANSFER");assertThat(reviewed.review()).isFalse();
    }
    @Test void outgoingReversalIsNotMistakenForARefund(){
        assertThat(classify("BANK","Estorno de entrada","Other","-30").kind()).isEqualTo("EXPENSE");
        assertThat(classify("BANK","C6 BANK","Loans","-465").review()).isTrue();
    }
}
