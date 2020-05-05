/*
 * The MIT License
 * Copyright © 2014-2019 Ilkka Seppälä
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.test.caching;

import com.test.caching.constants.CachingConstants;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;

public final class DbManager {

  private static MongoClient mongoClient;
  private static MongoDatabase db;
  private static boolean useMongoDB;

  private static Map<String, UserAccount> virtualDB;

  private DbManager() {
  }

  public static void createVirtualDb() {
    useMongoDB = false;
    virtualDB = new HashMap<>();
  }

  public static void connect() throws ParseException {
    useMongoDB = true;
    mongoClient = new MongoClient();
    db = mongoClient.getDatabase("test");
  }

  public static UserAccount readFromDb(String userId) {
    if (!useMongoDB) {
      if (virtualDB.containsKey(userId)) {
        return virtualDB.get(userId);
      }
      return null;
    }
    if (db == null) {
      try {
        connect();
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    var iterable = db
        .getCollection(CachingConstants.USER_ACCOUNT)
        .find(new Document(CachingConstants.USER_ID, userId));
    if (iterable == null) {
      return null;
    }
    Document doc = iterable.first();
    String userName = doc.getString(CachingConstants.USER_NAME);
    String appInfo = doc.getString(CachingConstants.ADD_INFO);
    return new UserAccount(userId, userName, appInfo);
  }

  public static void writeToDb(UserAccount userAccount) {
    if (!useMongoDB) {
      virtualDB.put(userAccount.getUserId(), userAccount);
      return;
    }
    if (db == null) {
      try {
        connect();
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    db.getCollection(CachingConstants.USER_ACCOUNT).insertOne(
        new Document(CachingConstants.USER_ID, userAccount.getUserId())
            .append(CachingConstants.USER_NAME, userAccount.getUserName())
            .append(CachingConstants.ADD_INFO, userAccount.getAdditionalInfo())
    );
  }

  public static void updateDb(UserAccount userAccount) {
    if (!useMongoDB) {
      virtualDB.put(userAccount.getUserId(), userAccount);
      return;
    }
    if (db == null) {
      try {
        connect();
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    db.getCollection(CachingConstants.USER_ACCOUNT).updateOne(
        new Document(CachingConstants.USER_ID, userAccount.getUserId()),
        new Document("$set", new Document(CachingConstants.USER_NAME, userAccount.getUserName())
            .append(CachingConstants.ADD_INFO, userAccount.getAdditionalInfo())));
  }

  public static void upsertDb(UserAccount userAccount) {
    if (!useMongoDB) {
      virtualDB.put(userAccount.getUserId(), userAccount);
      return;
    }
    if (db == null) {
      try {
        connect();
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    db.getCollection(CachingConstants.USER_ACCOUNT).updateOne(
        new Document(CachingConstants.USER_ID, userAccount.getUserId()),
        new Document("$set",
            new Document(CachingConstants.USER_ID, userAccount.getUserId())
                .append(CachingConstants.USER_NAME, userAccount.getUserName())
                .append(CachingConstants.ADD_INFO, userAccount.getAdditionalInfo())
        ),
        new UpdateOptions().upsert(true)
    );
  }
}
