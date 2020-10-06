import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Program extends JFrame {

    Program() {

        setLayout( new BorderLayout() );
        JSlider horSlider = new JSlider( 0, 360, 180 );
        add( horSlider, BorderLayout.SOUTH );
        JSlider verSlider = new JSlider( SwingConstants.VERTICAL, -90, 90, -90 );
        add( verSlider, BorderLayout.EAST );
        JPanel renderPanel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = ( Graphics2D) g;
                g2.setColor( Color.BLACK );
                g2.fillRect( 0, 0, getWidth(), getHeight() );

                List<Triangle> tris = new ArrayList<Triangle>();
                tris.add( new Triangle( new Vertex( 100, 100, 100 ),
                        new Vertex( -100, -100, 100 ),
                        new Vertex(-100, 100, -100 ),
                        Color.WHITE ) );
                tris.add( new Triangle( new Vertex( 100, 100, 100 ),
                        new Vertex( -100, -100, 100 ),
                        new Vertex(100, -100, -100 ),
                        Color.RED ) );
                tris.add( new Triangle( new Vertex( -100, 100, -100 ),
                        new Vertex( 100, -100, -100 ),
                        new Vertex(100, 100, 100 ),
                        Color.GREEN ) );
                tris.add( new Triangle( new Vertex( -100, 100, -100 ),
                        new Vertex( 100, -100, -100 ),
                        new Vertex(-100, -100, 100 ),
                        Color.BLUE ) );

                double heading = Math.toRadians( horSlider.getValue() );
                Matrix3 horTransform = new Matrix3( new double[] { Math.cos( heading ), 0, Math.sin( heading ),
                                                                                  0, 1, 0,
                                                                -Math.sin( heading ), 0, Math.cos( heading ) } );
                double pitch = Math.toRadians( verSlider.getValue() );
                Matrix3 verTransform = new Matrix3( new double[] { 1, 0, 0,
                                                                    0, Math.cos( pitch ), -Math.sin( pitch ),
                                                                    0, Math.sin( pitch ), Math.cos( pitch ) } );
                Matrix3 transform = horTransform.multiply( verTransform );

                BufferedImage image = new BufferedImage( getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB );

                double[] zBuffer = new double[ image.getWidth() * image.getHeight() ];

                for (int q = 0; q < zBuffer.length; q++) {

                    zBuffer[ q ] = Double.NEGATIVE_INFINITY;
                }

                for ( Triangle tr :
                     tris ) {

                    Vertex v1 = transform.transform( tr.getV1() );
                    Vertex v2 = transform.transform( tr.getV2() );
                    Vertex v3 = transform.transform( tr.getV3() );

                    v1.setX( v1.getX() + getWidth() / 2);
                    v1.setY( v1.getY() + getHeight() / 2);
                    v2.setX( v2.getX() + getWidth() / 2);
                    v2.setY( v2.getY() + getHeight() / 2);
                    v3.setX( v3.getX() + getWidth() / 2);
                    v3.setY( v3.getY() + getHeight() / 2);

                    int minX = ( int ) Math.max( 0, Math.ceil( Math.min( v1.getX(), Math.min( v2.getX(), v3.getX() ) ) ) );
                    int maxX = ( int ) Math.min( image.getWidth() - 1, Math.floor( Math.max( v1.getX(), Math.max( v2.getX(), v3.getX() ) ) ) );
                    int minY = ( int ) Math.max( 0, Math.ceil( Math.min( v1.getY(), Math.min( v2.getY(), v3.getY() ) ) ) );
                    int maxY = ( int ) Math.min( image.getHeight() - 1, Math.floor( Math.max( v1.getY(), Math.max( v2.getY(), v3.getY() ) ) ) );

                    double triangleArea = ( v1.getY() - v3.getY() ) * ( v2.getX() - v3.getX() ) +
                            ( v2.getY() - v3.getY() ) * ( v3.getX() - v1.getX() );

                    for (int y = minY; y <= maxY; y++) {

                        for (int x = minX; x <= maxX; x++) {

                            double b1 = ( ( y - v3.getY() ) * ( v2.getX() - v3.getX() ) +
                                    ( v2.getY() - v3.getY() ) * ( v3.getX() - x ) ) / triangleArea;
                            double b2 = ( ( y - v1.getY() ) * ( v3.getX() - v1.getX() ) +
                                    ( v3.getY() - v1.getY() ) * ( v1.getX() - x ) ) / triangleArea;
                            double b3 = ( ( y - v2.getY() ) * ( v1.getX() - v2.getX() ) +
                                    ( v1.getY() - v2.getY() ) * ( v2.getX() - x ) ) / triangleArea;

                            if ( b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1 ) {

                                double depth = b1 * v1.getZ() + b2 * v2.getZ() + b3 * v3.getZ();
                                int zIndex = y * image.getWidth() + x;
                                if( zBuffer[ zIndex ] < depth ) {

                                    double normX = ( v2.getY() - v1.getY() ) * ( v3.getZ() - v1.getZ() ) -
                                            ( v2.getZ() - v1.getZ() ) * ( v3.getY() - v1.getY() );
                                    double normY = ( v2.getZ() - v1.getZ() ) * ( v3.getX() - v1.getX() ) -
                                            ( v2.getX() - v1.getX() ) * ( v3.getZ() - v1.getZ() );
                                    double normZ = ( v2.getX() - v1.getX() ) * ( v3.getY() - v1.getY() ) -
                                            ( v2.getY() - v1.getY() ) * ( v3.getX() - v1.getX() );
                                    double normLength =  Math.sqrt( Math.pow( normX, 2 ) +Math.pow( normY, 2 ) +
                                            Math.pow( normZ, 2 ) );
                                    Vertex norm = new Vertex( normX / normLength, normY / normLength,
                                            normZ / normLength );
                                    double angCos = Math.abs( norm.getZ() );
                                    image.setRGB( x, y,  getAdvancedShadeColor( tr.getColor(), angCos ).getRGB() );
                                    zBuffer[ zIndex ] = depth;
                                }
                            }
                        }
                    }
                }

                g2.drawImage( image, 0, 0, null );
            }
        };
        horSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {

                renderPanel.repaint();
            }
        });

        verSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {

                renderPanel.repaint();
            }
        });
        add( renderPanel, BorderLayout.CENTER );
        setSize( 400, 400 );
        setVisible( true );
    }

    private Color getShadeColor ( Color color, double shade ) {

        int red = ( int )( color.getRed() * shade );
        int green = ( int )( color.getGreen() * shade );
        int blue = ( int )( color.getBlue() * shade );
        return new Color( red, green, blue );
    }

    private Color getAdvancedShadeColor ( Color color, double shade ) {

        double redLinear = Math.pow( color.getRed(), 2.4 ) * shade ;
        double greenLinear = Math.pow( color.getGreen(), 2.4 ) * shade ;
        double blueLinear = Math.pow( color.getBlue(), 2.4 ) * shade ;

        int red = ( int ) Math.pow( redLinear, 1 / 2.4 );
        int green = ( int ) Math.pow( greenLinear, 1 / 2.4 );
        int blue = ( int ) Math.pow( blueLinear, 1 / 2.4 );
        return new Color( red, green, blue );
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater( new Runnable() {

            @Override
            public void run() {

                new Program();
            }
        });
    }
}
