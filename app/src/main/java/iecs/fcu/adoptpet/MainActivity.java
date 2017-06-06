package iecs.fcu.adoptpet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PetArrayAdapter adapter = null;

    private static final int LIST_PETS = 1;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LIST_PETS: {
                    List<Pet> pets = (List<Pet>)msg.obj;
                    refreshPetList(pets);
                    break;
                }
            }
        }
    };

    private void refreshPetList(List<Pet> pets) {
        adapter.clear();
        adapter.addAll(pets);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lvPets = (ListView)findViewById(R.id.listview_pet);

        adapter = new PetArrayAdapter(this, new ArrayList<Pet>());
        lvPets.setAdapter(adapter);

        getPetsFromFirebase();
    }

    class FirebaseThread extends Thread {

        private DataSnapshot dataSnapshot;

        public FirebaseThread(DataSnapshot dataSnapshot) {
            this.dataSnapshot = dataSnapshot;
        }

        @Override
        public void run() {
            List<Pet> lsPets = new ArrayList<>();
            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                DataSnapshot dsSName = ds.child("shelter_name");
                DataSnapshot dsAKind = ds.child("animal_kind");

                String shelterName = (String)dsSName.getValue();
                String kind = (String)dsAKind.getValue();

                DataSnapshot dsImg = ds.child("album_file");
                String imgUrl = (String) dsImg.getValue();
                Bitmap petImg = getImgBitmap(imgUrl);

                Pet aPet = new Pet();
                aPet.setShelter(shelterName);
                aPet.setKind(kind);
                aPet.setImgUrl(petImg);
                lsPets.add(aPet);
                Log.v("AdoptPet", shelterName + ";" + kind);

                Message msg = new Message();
                msg.what = LIST_PETS;
                msg.obj = lsPets;
                handler.sendMessage(msg);
            }
        }
    }

    private void getPetsFromFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                new FirebaseThread(dataSnapshot).start();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.v("AdoptPet", databaseError.getMessage());
            }
        });
    }

    private Bitmap getImgBitmap(String imgUrl) {
        try {
            URL url = new URL(imgUrl);
            Bitmap bm = BitmapFactory.decodeStream(url.openConnection()
                    .getInputStream());
            return bm;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    class PetArrayAdapter extends ArrayAdapter<Pet> {
        Context context;

        public PetArrayAdapter(Context context, List<Pet> items) {
            super(context, 0, items);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            LinearLayout itemlayout = null;
            if (convertView == null) {
                itemlayout = (LinearLayout) inflater.inflate(R.layout.pet_item, null);
            } else {
                itemlayout = (LinearLayout) convertView;
            }
            Pet item = (Pet) getItem(position);
            TextView tvShelter = (TextView) itemlayout.findViewById(R.id.tv_shelter);
            tvShelter.setText(item.getShelter());
            TextView tvKind = (TextView) itemlayout.findViewById(R.id.tv_kind);
            tvKind.setText(item.getKind());
            ImageView ivPet = (ImageView) itemlayout.findViewById(R.id.iv_pet);
            ivPet.setImageBitmap(item.getImgUrl());
            return itemlayout;
        }
    }

}
