package jp.nyatla.nymmd.test;

import java.nio.*;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ComponentEvent;
import java.io.*;
import org.lwjgl.opengl.*;
import jp.nyatla.nymmd.*;
import glapp.*;

public class MmdTestLWJGL extends GLApp
{
	private long animation_start_time;
	private MmdPmdModel _pmd;
	private MmdVmdMotion _vmd;
	private MmdMotionPlayer _player;
	public IMmdPmdRender _render;
	private IMmdDataIo _data_io;
	public MmdTestLWJGL(File pmd_file,File vmd_file) throws FileNotFoundException,MmdException
	{
		super();
		//PMD
		FileInputStream fs = new FileInputStream(pmd_file);
		this._pmd = new MmdPmdModel(fs);
		//VMD
		FileInputStream fs2 = new FileInputStream(vmd_file);
		this._vmd = new MmdVmdMotion(fs2);
		this._data_io=new FileIO(pmd_file.getParentFile().getPath());
		window_title = "GLApp MmdTest";
        displayWidth = 800;
        displayHeight = 600;
	}
	private long prev_time = 0;
	public void setup()
	{
		// Player
		this._player = new MmdMotionPlayer(this._pmd, this._vmd);
		this._player.setLoop(true);
		try {
			this._render = new MmdPmdRenderLWJGL();
			this._render.setPmd(this._pmd,this._data_io);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.animation_start_time = System.currentTimeMillis();
		
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
		GL11.glCullFace(GL11.GL_FRONT);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.05f);

		FloatBuffer fLightPos = ByteBuffer.allocateDirect(4*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		fLightPos.put(new float[]{ 0.45f, 0.55f, 1.0f, 0.0f });
		fLightPos.flip();
		FloatBuffer fLightDif = ByteBuffer.allocateDirect(4*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		fLightDif.put(new float[]{ 0.9f, 0.9f, 0.9f, 0.0f });
		fLightDif.flip();
		FloatBuffer fLightAmb = ByteBuffer.allocateDirect(4*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		fLightAmb.put(new float[]{ 1.0f, 1.0f, 1.0f, 0.0f });
		fLightAmb.flip();
		FloatBuffer fLightSpq = ByteBuffer.allocateDirect(4*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		fLightSpq.put(new float[]{ 0.9f, 0.9f, 0.9f, 0.0f });
		fLightSpq.flip();

		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, fLightDif);
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_SPECULAR, fLightSpq);
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, fLightAmb);
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION, fLightPos);
		GL11.glEnable(GL11.GL_LIGHT1);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
	public void draw()
	{
		long iTime = System.currentTimeMillis() - this.animation_start_time;
		float fDiffTime = (float) (iTime - prev_time) * (1.0f / 30.0f);
		prev_time = iTime;
		try
		{
			this._player.updateMotion(fDiffTime);
		}
		catch(Exception e)
		{
		}
		//this._player.updateNeckBone(100.0f,10f,10f);
		this._render.updateSkinning(this._player.refSkinningMatrix());

		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT); // Clear the buffers for new frame.
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GL11.glTranslatef(0, -10, -30);
		GL11.glScalef(1.0f, 1.0f, 1.0f);

		GL11.glScalef(1.0f, 1.0f, -1.0f);
		this._render.render();
		GL11.glPopMatrix();
	}
	public void componentResized(ComponentEvent e)
	{
		int width = e.getComponent().getWidth();
		int height = e.getComponent().getHeight();
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glViewport(0, 0, width, height);

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glFrustum(-1.0f, 1.0f, -(float) height / (float) width, (float) height / (float) width, 1.0f, 100.0f);

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
	}
	public static void main(String[] args)
	{
		Frame input = new Frame();
		FileDialog fd;
		fd=new FileDialog(input, "Select PMD file" , FileDialog.LOAD);
		fd.setVisible(true);
		String pmd_file=fd.getDirectory()+fd.getFile();
		if(fd.getFile()==null){
			System.out.println("failed:please select pmd file.");
			input.dispose();return;
		}
		fd=new FileDialog(input, "Select VMD file" , FileDialog.LOAD);
		fd.setVisible(true);
		String vmd_file=fd.getDirectory()+fd.getFile();
		if(fd.getFile()==null){
			System.out.println("failed:please select vmd file.");
			input.dispose();return;
		}
		try
		{
		MmdTestLWJGL demo = new MmdTestLWJGL(new File(pmd_file),new File(vmd_file));
        demo.run();  // will call init(), render(), mouse event functions
		}
		catch(FileNotFoundException e)
		{
			System.err.println("File not Found:");
			System.err.println(e);
		}
		catch(MmdException e)
		{
			System.err.println("MmdException during run:");
			System.err.println(e);
		}
		return;
	}
}
class FileIO implements IMmdDataIo
{
	private String _dir;
	public FileIO(String i_dir)
	{
		this._dir=i_dir;
	}
	public InputStream request(String i_name)
	{
		FileInputStream fs2;
		try {
			fs2 = new FileInputStream(this._dir +"\\"+ i_name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return fs2;
	}
}