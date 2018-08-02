package de.upb.bibifi.verybest.common.util;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import de.upb.bibifi.verybest.common.models.*;

public class MoshiProvider {
    public static Moshi provideMoshi(){
        Class[] types = new Class[]{DepositAction.class, WithdrawAction.class, AccountCreateAction.class, GetBalanceAction.class};


        GenericPolymorphicJsonAdapterFactory.Builder builder = new GenericPolymorphicJsonAdapterFactory.Builder(Action.class)
                .setKey("type");
        for (Class type : types) {
            builder.map(type.getName(), type);
        }

        JsonAdapter.Factory polymorphicAdapterFactory = builder.build();

        return new Moshi.Builder()
                .add(new BigIntegerAdapter())
                .add(polymorphicAdapterFactory)
                .add(new AccountAdapter())
                .build();
    }
}
