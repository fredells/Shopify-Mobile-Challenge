package com.ells.shopifychallenge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShopifyChallenge extends ApplicationAdapter implements ApplicationListener {
	
	SpriteBatch batch;
	Texture img;
	private FileHandle orderData;
	
	private Skin skin;
	private Table table;
	private Stage stage;

	private Map<String, Array<Map<String, Object>>> orderPage = new HashMap<String, Array<Map<String, Object>>>();
	private Map<String, Object> order = new HashMap<String, Object>();
	private Map<String, Object> item = new HashMap<String, Object>();
	private Array<Map<String, Object>> orderArray = new Array<Map<String, Object>>();
	
	private String orders = "";
	private String amount = "";
	private String quantity = "";
	private String search = "";

	
	@Override
	public void create () {
		
		float screenWidth = Gdx.graphics.getWidth();
		float screenHeight = Gdx.graphics.getHeight();
		
		batch = new SpriteBatch();
		//img = new Texture("badlogic.jpg");
		
		skin = new Skin(Gdx.files.internal("data/uiskin.json"));
        stage = new Stage();
        table = new Table();
        
        table.setPosition(Gdx.graphics.getWidth() /2, Gdx.graphics.getHeight()/1.5f);
        
        //title logo
        Texture texture = new Texture(Gdx.files.internal("data/shopify-logo-white.png"));
        Image image = new Image(texture);
        table.add(image).size(screenWidth/2, screenWidth/8).colspan(2);
		table.row();
        
        final Label title = new Label("Search your Shopify orders by\n customer or product title", skin);
        title.setFontScale(3);
		table.add(title).pad(15).colspan(2);
		table.row();
        
		//search field
		TextField.TextFieldStyle textFieldStyle = skin.get(TextField.TextFieldStyle.class);
		textFieldStyle.font.getData().setScale(3.0f);
        final TextField textField = new TextField("", skin, "default");
        
        //search button
        TextButton button = new TextButton("Search", skin, "default");
        button.getLabel().setFontScale(3);
        
        table.add(textField).size(screenWidth/2, 130).right();
        table.add(button).left();
        
        final Label label = new Label("", skin);
        label.setFontScale(3);
		table.row();
		table.add(label).pad(15).colspan(2);
		
		
        button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				
				//run search and set label text
				searchBoth(textField.getText());
				//only display quantity if search was for a product name
				if (quantity.equals("0")) {
					label.setText("Orders: " + orders + "\n" + "Amount: " + amount);
				}
				else {
					label.setText("Orders: " + orders + "\n" + "Quantity: " + quantity + "\n" + "Amount: " + amount);
				}
				
			}
		});
        
        
        
        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
		
	}

	//search for either a customer or product name
public void searchBoth(String productName) {
	
	int orders = 0;
	int quantity = 0;
	float totalSales = 0;
	//System.out.println("Searching for: " + productName);
	
	this.search = productName;
	//reduce everything to lower case for search
	productName = productName.toLowerCase();
	
	GetExample get = new GetExample();
	try {
		//get order page
		String getData = get.run("https://shopicruit.myshopify.com/admin/orders.json?page=1&access_token=c32313df0d0ef512ca64d5b336a0d7c6");
		//System.out.println(getData);
		
		//parse json
		JsonValue jsonReader = new JsonReader().parse(getData);
		JsonValue jsonArray = jsonReader.get("orders");
		
		Json json = new Json();
		
		for (JsonValue jsonOrder : jsonArray.iterator()) {
			
			//System.out.println(jsonOrder.size);
			//System.out.println(jsonOrder);
			order = json.fromJson(HashMap.class, jsonOrder.toString());
			//System.out.println(order.get("billing_address"));
			
			if (order.get("line_items")!= null) {
			//System.out.println(((JsonValue) order.get("billing_address")).getString("first_name"));
				for (int j = 0; j < ((Array) order.get("line_items")).size; j++) {
					//check product
					if (((String) (((JsonValue) ((Array) order.get("line_items")).get(j)).getString("title"))).toLowerCase().contains(productName)) {
						quantity += ((int) (((JsonValue) ((Array) order.get("line_items")).get(j)).getInt("quantity")));
						if (quantity > 0) {
							orders++;
						}
						totalSales += ((int) (((JsonValue) ((Array) order.get("line_items")).get(j)).getInt("quantity")))
								* ((float) (((JsonValue) ((Array) order.get("line_items")).get(j)).getFloat("price")));
						//System.out.println(((float) (((JsonValue) ((Array) order.get("line_items")).get(j)).getInt("price"))));
						
	
					}
				}
				
					//check name
					if (order.get("billing_address")!= null) {
						String concat = (String) (((JsonValue) order.get("billing_address")).getString("first_name")) + " " + (String) (((JsonValue) order.get("billing_address")).getString("last_name"));
						//System.out.println(concat);
						if (concat.toLowerCase().contains(productName)) {
							orders++;
							totalSales += (float) order.get("total_price");
							quantity = 0;
						}
					}
				
			}
			
			
		}
		
		//System.out.println("Orders: " + orders + "\n" + "Total Sales : " + totalSales);
		this.orders = String.valueOf(orders);
		this.amount = String.format("%.2f", totalSales);
		this.quantity = String.valueOf(quantity);
		

		
	}
	catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0.02353f, 0.2235f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		stage.draw();
		batch.end();
	}
}
